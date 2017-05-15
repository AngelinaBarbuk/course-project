package by.fpm.barbuk.uploadBigFile;

import by.fpm.barbuk.account.Account;
import by.fpm.barbuk.dropbox.DropboxHelper;
import by.fpm.barbuk.google.drive.GoogleHelper;
import by.fpm.barbuk.utils.EncryptHelper;
import by.fpm.barbuk.utils.EncryptedFile;
import by.fpm.barbuk.yandex.YandexHelper;
import com.temboo.core.TembooException;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

@Component
@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class BigFileHelper {


    public static final String DROPBOX = "dropbox";
    public static final String GOOGLE = "google";
    public static final String YANDEX = "yandex";
    @Autowired
    private DropboxHelper dropboxHelper;
    @Autowired
    private GoogleHelper googleHelper;
    @Autowired
    private YandexHelper yandexHelper;
    private EncryptHelper encryptHelper = new EncryptHelper();

    public BigFileHelper() throws NoSuchPaddingException, NoSuchAlgorithmException {
    }

    public void uploadFile(MultipartFile file, Account account) throws IOException, TembooException, JSONException {
        System.out.println(Runtime.getRuntime().maxMemory());
        BigFile bigFile = new BigFile();
        bigFile.setFileName(getFileName(file));
        bigFile.setType(file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".") + 1));
        bigFile.setSize(file.getSize());
        bigFile.setContentType(file.getContentType());

        long uploadedSize = 0;
        List<String> clouds = collectClouds(account);
        try {
            for (int i = 0; uploadedSize < file.getSize(); i++) {
                long filepartSize = generatePartSize(uploadedSize, file.getSize());
                String cloud = generateCloudUser(clouds);
                FilePart filePart = new FilePart();
                filePart.setCloudName(cloud);
                filePart.setSize(filepartSize);
                byte[] content = new byte[(int) filepartSize];
                file.getInputStream().read(content, 0, (int) filepartSize);
                MultipartFile multipartFile = new MockMultipartFile(getFileName(bigFile, i), getFileName(bigFile, i), file.getContentType(), content);
                System.out.println(cloud + "   " + filepartSize);
                EncryptedFile encrypted = encryptHelper.encrypt(multipartFile);
                DownloadUploadFile helper = getHelper(cloud);
                String path = helper.uploadFile(encrypted.getFile(), getPath(cloud), account);
                filePart.setPath(path);
                filePart.setSecretKey(encrypted.getSecretKey());
                bigFile.getParts().add(filePart);
                uploadedSize += filepartSize;
            }
            account.getUserBigFiles().getBigFiles().add(bigFile);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private String getFileName(BigFile bigFile, int i) {
        return bigFile.getFileName() + bigFile.getId() + "filepart" + i + "." + bigFile.getType();
    }

    public MultipartFile downloadFile(String id, Account account) throws IOException, TembooException, JSONException {
        BigFile bigFile = getBigFile(account, id);

        Vector<InputStream> streams = new Vector<>(bigFile.getParts().size());
        if (bigFile != null) {
            long downloadedSize = 0;
            for (FilePart filePart : bigFile.getParts()) {
                DownloadUploadFile helper = getHelper(filePart.getCloudName());
                MultipartFile multipartFile = helper.downloadFile(filePart.getPath(), account);
                MultipartFile decrypted = encryptHelper.decrypt(filePart.getSecretKey(), multipartFile);
                streams.add(decrypted.getInputStream());
                downloadedSize += multipartFile.getSize();
            }
            if (downloadedSize == bigFile.getSize()) {
                SequenceInputStream inputStream = new SequenceInputStream(streams.elements());
                return new MockMultipartFile(bigFile.getFileName() + "." + bigFile.getType(), bigFile.getFileName() + "." + bigFile.getType(), bigFile.getContentType(), inputStream);
            }
        }
        return null;
    }

    private String getAccessToken(String cloud, Account account) {
        if (GOOGLE.equals(cloud)) {
            return account.getGoogleUser().getAccessToken();
        }
        return null;
    }

    private String getFileName(MultipartFile file) {
        return file.getOriginalFilename().substring(0, file.getOriginalFilename().lastIndexOf("."));
    }

    public List<String> collectClouds(Account account) {
        List<String> clouds = new ArrayList<>();
        if (account.getDropboxUser() != null) {
            clouds.add(DROPBOX);
        }
        if (account.getGoogleUser() != null) {
            clouds.add(GOOGLE);
        }
        if (account.getYandexUser() != null) {
            clouds.add(YANDEX);
        }
        return clouds;
    }

    private String generateCloudUser(List<String> cloudUsers) {
        if (cloudUsers.isEmpty()) {
            return null;
        }
        int n = (int) (cloudUsers.size() * Math.random());
        return cloudUsers.get(n);
    }

    private long generatePartSize(long uploadedSize, long fileSize) {
        if (fileSize < 10000)
            return fileSize - uploadedSize;
        long size = Math.min(fileSize - uploadedSize, (long) (Math.random() * Math.min(fileSize / 4., 1_000_000_000L)));
        return Math.min(size, 1_000_000_000);
    }

    private DownloadUploadFile getHelper(String cloud) {
        switch (cloud) {
            case DROPBOX: {
                return dropboxHelper;
            }
            case GOOGLE: {
                return googleHelper;
            }
            case YANDEX: {
                return yandexHelper;
            }
        }
        return null;
    }

    private String getPath(String cloud) {
        switch (cloud) {
            case YANDEX: {
                return "disk:/";
            }
            default: {
                return "root";
            }
        }
    }

    public DropboxHelper getDropboxHelper() {
        return dropboxHelper;
    }

    public void setDropboxHelper(DropboxHelper dropboxHelper) {
        this.dropboxHelper = dropboxHelper;
    }

    public GoogleHelper getGoogleHelper() {
        return googleHelper;
    }

    public void setGoogleHelper(GoogleHelper googleHelper) {
        this.googleHelper = googleHelper;
    }

    public YandexHelper getYandexHelper() {
        return yandexHelper;
    }

    public void setYandexHelper(YandexHelper yandexHelper) {
        this.yandexHelper = yandexHelper;
    }

    public boolean delete(String path, Account account) {
        BigFile bigFile = getBigFile(account, path);
        if (bigFile != null) {
            for (FilePart filePart : bigFile.getParts()) {
                DownloadUploadFile helper = getHelper(filePart.getCloudName());
                helper.delete(filePart.getPath(), account);
            }
        }
        account.getUserBigFiles().getBigFiles().remove(bigFile);
        return true;
    }

    private BigFile getBigFile(Account account, String id) {
        for (BigFile bf : account.getUserBigFiles().getBigFiles()) {
            if (bf.getId().equals(id)) {
                return bf;
            }
        }
        return null;
    }

    public EncryptHelper getEncryptHelper() {
        return encryptHelper;
    }

    public void setEncryptHelper(EncryptHelper encryptHelper) {
        this.encryptHelper = encryptHelper;
    }
}
