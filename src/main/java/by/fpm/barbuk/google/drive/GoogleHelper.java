package by.fpm.barbuk.google.drive;

import by.fpm.barbuk.account.Account;
import by.fpm.barbuk.account.CloudUser;
import by.fpm.barbuk.cloudEntities.CloudFile;
import by.fpm.barbuk.cloudEntities.CloudFolder;
import by.fpm.barbuk.cloudEntities.FolderList;
import by.fpm.barbuk.temboo.CloudHelper;
import by.fpm.barbuk.temboo.TembooHelper;
import com.google.api.client.auth.oauth2.*;
import com.google.api.client.http.BasicAuthentication;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.FileList;
import com.temboo.core.TembooException;
import javafx.util.Pair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public final class GoogleHelper extends TembooHelper implements CloudHelper {

    public static final String MIME_TYPE_FOLDER = "application/vnd.google-apps.folder";
    public static final String CLIENT_ID = "569486570902-vrcv37j4msm28ucb22024aseesf8s4is.apps.googleusercontent.com";
    public static final String CLIENT_SECRET = "giRKgPH6v0eCPMDIEhrcKVBh";
    private static final String FORWARDING_URL = "http://localhost:8080/google/OAuthLogIn";
    private static final String SCOPE = "https://www.googleapis.com/auth/drive";
    private static Map<String, String> folders = new HashMap<>();
    private Drive SERVICE;

    public static Drive getDriveService(Credential credential) throws IOException {
        return new Drive.Builder(
                new NetHttpTransport(), new JacksonFactory(), credential)
                .setApplicationName("course")
                .build();
    }

    protected AuthorizationCodeFlow initializeFlow() throws IOException {
        return new AuthorizationCodeFlow.Builder(BearerToken.authorizationHeaderAccessMethod(),
                new NetHttpTransport(),
                new JacksonFactory(),
                new GenericUrl("https://server.example.com/token"),
                new BasicAuthentication("s6BhdRkqt3", "7Fjfp0ZBr1KtDRbnfVdmIw"),
                "s6BhdRkqt3",
                "https://server.example.com/authorize").setCredentialDataStore(
                StoredCredential.getDefaultDataStore(
                        new FileDataStoreFactory(new File("datastoredir"))))
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

    private void generateStateToken() {
        SecureRandom random = new SecureRandom();
        stateToken = "google-" + random.nextInt();
    }

    public String getStateToken() {
        return stateToken;
    }

    @Override
    public CloudUser getUserInfo() throws IOException {
        return null;
    }

    public GoogleUser getUserInfo(String url) throws IOException {
        AuthorizationCodeResponseUrl authResp = new AuthorizationCodeResponseUrl(url);
        if (authResp.getError() == null) {
            String code = authResp.getCode();
            TokenResponse tokenResponse = new AuthorizationCodeTokenRequest(new NetHttpTransport(), new JacksonFactory(),
                    new GenericUrl("https://www.googleapis.com/oauth2/v4/token"), code)
                    .setRedirectUri(FORWARDING_URL)
                    .setClientAuthentication(new BasicAuthentication(CLIENT_ID, CLIENT_SECRET)).execute();
            GoogleUser googleUser = new GoogleUser();
            googleUser.setAccessToken(tokenResponse.getAccessToken());
            googleUser.setRefreshToken(tokenResponse.getRefreshToken());
            Credential credential = (Credential) new Credential.Builder(BearerToken.authorizationHeaderAccessMethod())
                    .setTransport(new NetHttpTransport())
                    .setJsonFactory(new JacksonFactory())
                    .setTokenServerUrl(new GenericUrl("https://www.googleapis.com/oauth2/v4/token"))
                    .setClientAuthentication(new BasicAuthentication(CLIENT_ID, CLIENT_SECRET))
                    .build()
                    .setFromTokenResponse(tokenResponse);
            SERVICE = getDriveService(credential);
            return googleUser;
        }
        return null;
    }

    private List<Pair<String, String>> getParents(String path, GoogleUser user) throws TembooException, JSONException, IOException {

        com.google.api.services.drive.model.File result = SERVICE.files().get(path).setFields("parents").execute();
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

    public CloudFolder getFolderContent(String path, Account account) throws TembooException, JSONException, UnsupportedEncodingException {
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
            for (com.google.api.services.drive.model.File file : result.getFiles()) {
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

    public FolderList getFolders(String path, Account account) throws TembooException, JSONException, UnsupportedEncodingException {
        GoogleUser user = account.getGoogleUser();
        FolderList folderList = new FolderList();
        com.temboo.Library.Google.Drive.Files.List filesListChoreo = new com.temboo.Library.Google.Drive.Files.List(session);
        com.temboo.Library.Google.Drive.Files.List.ListInputSet filesListInputs = filesListChoreo.newInputSet();

        filesListInputs.set_ClientID(CLIENT_ID);
        filesListInputs.set_ClientSecret(CLIENT_SECRET);
        filesListInputs.set_AccessToken(user.getAccessToken());
        filesListInputs.setInput("corpus", "domain");
        filesListInputs.set_MaxResults(1000);
        filesListInputs.set_Query("'" + path + "' in parents and mimeType = '" + MIME_TYPE_FOLDER + "'");
        try {
            com.temboo.Library.Google.Drive.Files.List.ListResultSet filesListResults;
            try {
                filesListResults = filesListChoreo.execute(filesListInputs);
            } catch (TembooException e) {
                filesListInputs.set_RefreshToken(user.getRefreshToken());
                filesListResults = filesListChoreo.execute(filesListInputs);
            }
            if (filesListResults.getException() == null) {
                String newAccessToken = filesListResults.get_NewAccessToken();
                if (newAccessToken != null && !newAccessToken.isEmpty()) {
                    user.setAccessToken(newAccessToken);
                    account.setGoogleUser(user);
                }
                JSONObject result = new JSONObject(filesListResults.get_Response());
                JSONArray content = result.getJSONArray("items");
                List<CloudFile> folders = new ArrayList<>();
                for (int i = 0; i < content.length(); i++) {
                    CloudFile cloudFile = new CloudFile();
                    JSONObject object = content.getJSONObject(i);
                    cloudFile.setPath(object.getString("id"));
                    cloudFile.setShowName(object.getString("title"));
                    cloudFile.setDir(object.getString("mimeType").equals(MIME_TYPE_FOLDER) ? true : false);
                    folders.add(cloudFile);
                    this.folders.put(cloudFile.getPath(), cloudFile.getShowName());
                }
                folderList.setFolders(folders);
                List<Pair<String, String>> pairs = getParents(path, user);
                if (pairs.isEmpty())
                    folderList.setPrevFolder("");
                else
                    folderList.setPrevFolder(pairs.get(pairs.size() - 1).getKey());
                return folderList;
            }
        } catch (TembooException ex) {
            System.out.println("empty");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return folderList;
    }

    @Override
    public String getDownloadFileLink(String path, Account account, boolean isFileContent) throws TembooException, JSONException {
        return null;
    }

    public String getDownloadFileLink(String path, Account account) throws TembooException, JSONException {
        GoogleUser user = account.getGoogleUser();
        try {
            com.google.api.services.drive.model.File file = SERVICE.files().get(path).setFields("webContentLink").execute();
            return file.getWebContentLink();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public boolean delete(String path, Account account) throws TembooException, JSONException {
        GoogleUser user = account.getGoogleUser();
        try {
            SERVICE.files().delete(path).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public boolean createFolder(String path, String folderName, Account account) throws TembooException, JSONException {
        try {
            com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
            fileMetadata.setName(folderName);
            fileMetadata.setMimeType("application/vnd.google-apps.folder");
            List<String> parents = new ArrayList<>();
            parents.add(path);
            fileMetadata.setParents(parents);
            com.google.api.services.drive.model.File file = SERVICE.files().create(fileMetadata)
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;

    }

    public String uploadFile(MultipartFile file, String path, Account account) throws TembooException, IOException, JSONException {
        GoogleUser user = account.getGoogleUser();
        com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
        fileMetadata.setName(file.getOriginalFilename());
        List<String> parents = new ArrayList<>();
        parents.add(path);
        fileMetadata.setParents(parents);

        FileContent mediaContent = new FileContent(file.getContentType(), multipartToFile(file));
        com.google.api.services.drive.model.File result = SERVICE.files().create(fileMetadata, mediaContent)
                .set("uploadType", "resumable")
                .execute();
        return "";
    }

    @Override
    public long getAvailableSize(Account account) {

        return 0;
    }

    public File multipartToFile(MultipartFile multipart) throws IllegalStateException, IOException {
        File convFile = new File(multipart.getOriginalFilename());
        multipart.transferTo(convFile);
        return convFile;
    }

}
