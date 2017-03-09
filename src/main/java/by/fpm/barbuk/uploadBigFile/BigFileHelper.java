package by.fpm.barbuk.uploadBigFile;

import by.fpm.barbuk.account.Account;
import by.fpm.barbuk.account.CloudUser;
import by.fpm.barbuk.dropbox.DropboxHelper;
import by.fpm.barbuk.temboo.CloudHelper;
import com.temboo.core.TembooException;
import org.json.JSONException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class BigFileHelper {


    public static final String DROPBOX = "dropbox";
    public static final String GOOGLE = "google";
    private static Random random = new Random(3);

    public void uploadFile(MultipartFile file, Account account) throws IOException, TembooException, JSONException {
        BigFile bigFile = new BigFile();
        bigFile.setFileName(file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".") + 1));
        bigFile.setType(file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".") + 1));

        int n = random.nextInt();
        for (int i = 0; i <= n; i++) {

        }

        FilePart filePart = new FilePart();
        filePart.setCloudName(DROPBOX);
        filePart.setFile(file);
        filePart.setPath(bigFile.getFileName() + "-filepart.txt");

        MultipartFile multipartFile = new MockMultipartFile(file.getOriginalFilename() + "filepart", file.getOriginalFilename() + "filepart", "text/plain", file.getInputStream());
        CloudHelper dropboxHelper = new DropboxHelper();
        dropboxHelper.uploadFile(multipartFile, "root", account);

        bigFile.getParts().add(filePart);
        account.getUserBigFiles().getBigFiles().add(bigFile);
    }

    private List<CloudUser> collectClouds(Account account) {
        List<CloudUser> clouds = new ArrayList<>();
        if (account.getDropboxUser() != null) {
            clouds.add(account.getDropboxUser());
        }
        if (account.getGoogleUser() != null) {
            clouds.add(account.getGoogleUser());
        }
        return clouds;
    }

    private CloudUser generateCloudUser(List<CloudUser> cloudUsers) {
        if (cloudUsers.isEmpty()) {
            return null;
        }
        int n = (int) (cloudUsers.size() * Math.random());
        return cloudUsers.get(n);
    }

    private int generatePartSize(int len) {
        return (int) (Math.random() * len / 2.);
    }
}
