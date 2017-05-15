package by.fpm.barbuk.google.drive;

import by.fpm.barbuk.account.Account;
import by.fpm.barbuk.cloudEntities.CloudFile;
import by.fpm.barbuk.cloudEntities.CloudFolder;
import by.fpm.barbuk.uploadBigFile.DownloadUploadFile;
import com.google.api.client.auth.oauth2.*;
import com.google.api.client.http.BasicAuthentication;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import javafx.util.Pair;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class GoogleHelper implements DownloadUploadFile {

    public static final String MIME_TYPE_FOLDER = "application/vnd.google-apps.folder";
    public static final String CLIENT_ID = "569486570902-vrcv37j4msm28ucb22024aseesf8s4is.apps.googleusercontent.com";
    public static final String CLIENT_SECRET = "giRKgPH6v0eCPMDIEhrcKVBh";
    private static final String FORWARDING_URL = "http://localhost:8080/google/OAuthLogIn";
    private static final String SCOPE = "https://www.googleapis.com/auth/drive";
    public static final String TOKEN_URL = "https://www.googleapis.com/oauth2/v4/token";
    private static Map<String, String> folders = new HashMap<>();
    private Drive SERVICE;
    private Credential credential;

    public GoogleHelper() {
        System.out.println("new google helper");
    }

    public static Drive getDriveService(Credential credential) throws IOException {
        return new Drive.Builder(
                new NetHttpTransport(), new JacksonFactory(), credential)
                .setApplicationName("course")
                .build();
    }

    public String getLoginUrl() {
        List<String> scopes = new ArrayList<>();
        scopes.add(SCOPE);
        String url = new AuthorizationCodeRequestUrl(
                "https://accounts.google.com/o/oauth2/v2/auth", CLIENT_ID).setScopes(scopes)
                .setRedirectUri(FORWARDING_URL).build();
        return url;

    }

    public GoogleUser getUserInfo(String url) throws IOException {
        AuthorizationCodeResponseUrl authResp = new AuthorizationCodeResponseUrl(url);
        if (authResp.getError() == null) {
            String code = authResp.getCode();
            TokenResponse tokenResponse = new AuthorizationCodeTokenRequest(new NetHttpTransport(), new JacksonFactory(),
                    new GenericUrl(TOKEN_URL), code)
                    .setRedirectUri(FORWARDING_URL)
                    .setClientAuthentication(new BasicAuthentication(CLIENT_ID, CLIENT_SECRET)).execute();
            GoogleUser googleUser = new GoogleUser();
            googleUser.setAccessToken(tokenResponse.getAccessToken());
            googleUser.setRefreshToken(tokenResponse.getRefreshToken());
            setCredential(new Credential.Builder(BearerToken.authorizationHeaderAccessMethod())
                    .setTransport(new NetHttpTransport())
                    .setJsonFactory(new JacksonFactory())
                    .setTokenServerUrl(new GenericUrl("https://www.googleapis.com/oauth2/v4/token"))
                    .setClientAuthentication(new BasicAuthentication(CLIENT_ID, CLIENT_SECRET))
                    .build()
                    .setFromTokenResponse(tokenResponse));
            SERVICE = getDriveService(credential);
            return googleUser;
        }
        return null;
    }

    public boolean authorize(Account account) {
        try {
            GoogleUser user = account.getGoogleUser();
            setCredential(new Credential.Builder(BearerToken.authorizationHeaderAccessMethod())
                    .setTransport(new NetHttpTransport())
                    .setJsonFactory(new JacksonFactory())
                    .setTokenServerUrl(new GenericUrl("https://www.googleapis.com/oauth2/v4/token"))
                    .setClientAuthentication(new BasicAuthentication(CLIENT_ID, CLIENT_SECRET))
                    .build()
                    .setAccessToken(user.getAccessToken()));
            SERVICE = getDriveService(credential);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    private List<Pair<String, String>> getParents(String path, GoogleUser user) throws IOException {

        File result = SERVICE.files().get(path).setFields("parents").execute();
        result.getParents();
        if (result.getParents() != null) {
            List<Pair<String, String>> pairs = new ArrayList<>();
            for (String parent : result.getParents()) {
                if (folders.containsKey(parent)) {
                    pairs.add(new Pair<>(parent, folders.get(parent)));
                } else /*if (item.getBoolean("isRoot")) */ {
                    pairs.add(new Pair<>("root", "root"));
                }
            }
            return pairs;
        }
        return new ArrayList<>();
    }

    public CloudFolder getFolderContent(String path, Account account) {
        GoogleUser user = account.getGoogleUser();
        try {
            FileList result = SERVICE.files().list()
                    .setCorpus("domain")
                    .setQ("'" + path + "' in parents")
                    .execute();
            CloudFolder cloudFolder = new CloudFolder();
            cloudFolder.setCurrentPath(path);
            cloudFolder.setShowName(path);
            cloudFolder.setPath(getParents(path, user));

            List<CloudFile> items = new ArrayList<>();
            List<CloudFile> folders = new ArrayList<>();
            List<CloudFile> files = new ArrayList<>();
            for (File file : result.getFiles()) {
                CloudFile cloudFile = new CloudFile();
                cloudFile.setPath(file.getId());
                cloudFile.setShowName(file.getName());
                cloudFile.setDir(file.getMimeType().equals(MIME_TYPE_FOLDER) ? true : false);
                if (cloudFile.isDir()) {
                    folders.add(cloudFile);
                    this.folders.put(cloudFile.getPath(), cloudFile.getShowName());
                } else {
                    cloudFile.setFileType(file.getName().substring(file.getName().lastIndexOf(".") + 1));
                    files.add(cloudFile);
                }
            }
            cloudFolder.setContent(items);
            cloudFolder.setFiles(files);
            cloudFolder.setFolders(folders);
            return cloudFolder;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new CloudFolder();
    }


    public String getDownloadFileLink(String path, Account account) {
        GoogleUser user = account.getGoogleUser();
        try {
            File file = SERVICE.files().get(path).setFields("webContentLink").execute();
            return file.getWebContentLink();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    @Override
    public boolean delete(String path, Account account) {
        GoogleUser user = account.getGoogleUser();
        try {
            SERVICE.files().delete(path).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public boolean createFolder(String path, String folderName, Account account) {
        try {
            File fileMetadata = new File();
            fileMetadata.setName(folderName);
            fileMetadata.setMimeType("application/vnd.google-apps.folder");
            List<String> parents = new ArrayList<>();
            parents.add(path);
            fileMetadata.setParents(parents);
            File file = SERVICE.files().create(fileMetadata)
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;

    }

    @Override
    public MultipartFile downloadFile(String path, Account account) {
        InputStream in = null;
        for(int i = 0; i<CHUNKED_UPLOAD_MAX_ATTEMPTS; i++) {
            try {
                in = SERVICE.files().get(path).executeMediaAsInputStream();
                File file = SERVICE.files().get(path).execute();
                MultipartFile multipartFile =
                        new MockMultipartFile(file.getName(), file.getName(), file.getMimeType(), in);
                return multipartFile;
            }catch (IOException e) {
                continue;
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return null;
    }

    @Override
    public String uploadFile(MultipartFile file, String path, Account account) {
        GoogleUser user = account.getGoogleUser();
        File fileMetadata = new File();
        fileMetadata.setName(file.getOriginalFilename());
        List<String> parents = new ArrayList<>();
        parents.add(path);
        fileMetadata.setParents(parents);

        for(int i=0; i<CHUNKED_UPLOAD_MAX_ATTEMPTS; i++) {
            try {
                FileContent mediaContent = new FileContent(file.getContentType(), multipartToFile(file));
                File result = SERVICE.files()
                        .create(fileMetadata, mediaContent)
                        .set("uploadType", "resumable")
                        .execute();
                return result.getId();
            } catch (IOException e) {
                continue;
            }
        }
        return "";
    }

    public long getAvailableSize(Account account) {

        return 0;
    }

    public java.io.File multipartToFile(MultipartFile multipart) throws IllegalStateException, IOException {
        java.io.File convFile = new java.io.File(multipart.getOriginalFilename());
        multipart.transferTo(convFile);
        return convFile;
    }

    public Credential getCredential() {
        return credential;
    }

    public void setCredential(Credential credential) {
        this.credential = credential;
    }
}
