package by.fpm.barbuk.uploadBigFile;

import by.fpm.barbuk.MoveController;
import by.fpm.barbuk.account.Account;
import by.fpm.barbuk.dropbox.DropboxHelper;
import by.fpm.barbuk.google.drive.GoogleHelper;
import by.fpm.barbuk.temboo.CloudHelper;
import com.temboo.core.TembooException;
import org.json.JSONException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class BigFileHelper {


    public static final String DROPBOX = "dropbox";
    public static final String GOOGLE = "google";

    public void uploadFile(MultipartFile file, Account account) throws IOException, TembooException, JSONException {
        BigFile bigFile = new BigFile();
        bigFile.setFileName(getFileName(file));
        bigFile.setType(file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".") + 1));
        bigFile.setSize(file.getSize());

        long uploadedSize = 0;
        List<String> clouds = collectClouds(account);
        for (int i = 0; uploadedSize < file.getSize(); i++) {
            long filepartSize = generatePartSize(uploadedSize, file.getSize());
            String cloud = generateCloudUser(clouds);
            FilePart filePart = new FilePart();
            filePart.setCloudName(cloud);
            filePart.setSize(filepartSize);
            byte[] content = new byte[(int) filepartSize];
            System.arraycopy(file.getBytes(), (int) (uploadedSize), content, 0, (int) (filepartSize));
            MultipartFile multipartFile = new MockMultipartFile(file.getOriginalFilename() + bigFile.getId() + "filepart" + i, file.getOriginalFilename() + bigFile.getId() + "filepart" + i, "text/plain", content);
            CloudHelper helper = getHelper(cloud);
            filePart.setPath(helper.uploadFile(multipartFile, "root", account));
            bigFile.getParts().add(filePart);
            uploadedSize += filepartSize;
        }
        account.getUserBigFiles().getBigFiles().add(bigFile);
    }

    public void downloadFile(String id, Account account) throws IOException, TembooException, JSONException {
        BigFile bigFile = null;
        for (BigFile bf : account.getUserBigFiles().getBigFiles()) {
            if (bf.getId().equals(id)) {
                bigFile = bf;
                break;
            }
        }
        if (bigFile != null) {
            long downloadedSize = 0;
            byte[] fileBytes = new byte[(int) bigFile.getSize()];
            for (FilePart filePart : bigFile.getParts()) {
                CloudHelper helper = getHelper(filePart.getCloudName());
                String link = helper.getDownloadFileLink(filePart.getPath(), account, false);
                MultipartFile multipartFile = MoveController.downloadFile(link /*+ (filePart.getCloudName().equals(GOOGLE) ? "&alt=media" : "")*/);
                System.arraycopy(multipartFile.getBytes(), 0, fileBytes, (int) downloadedSize, (int) (multipartFile.getSize()));
                downloadedSize += multipartFile.getSize();
            }
            int a = 0;
        }
    }

    private String getFileName(MultipartFile file) {
        return file.getOriginalFilename().substring(0, file.getOriginalFilename().lastIndexOf("."));
    }

    private List<String> collectClouds(Account account) {
        List<String> clouds = new ArrayList<>();
        if (account.getDropboxUser() != null) {
            clouds.add(DROPBOX);
        }
        if (account.getGoogleUser() != null) {
            clouds.add(GOOGLE);
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
        long size = 0;
        if (uploadedSize >= fileSize * 3 / 4) {
            size = fileSize - uploadedSize;
        } else {
            size = (long) (fileSize * Math.random() / 4.);
        }
        return size;
    }

    private CloudHelper getHelper(String cloud) {
        switch (cloud) {
            case DROPBOX: {
                return new DropboxHelper();
            }
            case GOOGLE: {
                return new GoogleHelper();
            }
            default: {
                return new DropboxHelper();
            }
        }
    }
}
