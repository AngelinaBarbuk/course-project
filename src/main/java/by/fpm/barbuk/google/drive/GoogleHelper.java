package by.fpm.barbuk.google.drive;

import by.fpm.barbuk.account.Account;
import by.fpm.barbuk.cloudEntities.CloudFile;
import by.fpm.barbuk.cloudEntities.CloudFolder;
import by.fpm.barbuk.cloudEntities.FolderList;
import by.fpm.barbuk.temboo.CloudHelper;
import by.fpm.barbuk.temboo.TembooHelper;
import com.sun.org.apache.xml.internal.security.utils.Base64;
import com.temboo.Library.Google.Drive.Files.Delete;
import com.temboo.Library.Google.Drive.Files.Get;
import com.temboo.Library.Google.Drive.Files.Insert;
import com.temboo.Library.Google.OAuth.FinalizeOAuth;
import com.temboo.Library.Google.OAuth.InitializeOAuth;
import com.temboo.core.TembooException;
import javafx.util.Pair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public final class GoogleHelper extends TembooHelper implements CloudHelper {

    public static final String MIME_TYPE_FOLDER = "application/vnd.google-apps.folder";
    // Provide your dropbox App ID and App Secret.
    private static final String APP_ID = "mvfavfx9yg4ts62";
    private static final String APP_SECRET = "2g8wfrnbdpuc1of";
    // Callback URI that Temboo will redirect to after successful authentication.
    private static final String FORWARDING_URL = "http://localhost:8080/google/OAuthLogIn";
    private static final String CLIENT_ID = "569486570902-vrcv37j4msm28ucb22024aseesf8s4is.apps.googleusercontent.com";
    private static final String SCOPE = "https://www.googleapis.com/auth/drive";
    private static final String CLIENT_SECRET = "giRKgPH6v0eCPMDIEhrcKVBh";
    private static Map<String, String> folders = new HashMap<>();

    public String getLoginUrl() {
        String authURL = "";
        try {
            InitializeOAuth initializeOAuthChoreo = new InitializeOAuth(session);
            InitializeOAuth.InitializeOAuthInputSet initializeOAuthInputs = initializeOAuthChoreo.newInputSet();

            initializeOAuthInputs.set_Scope(SCOPE);
            initializeOAuthInputs.set_ClientID(CLIENT_ID);
            initializeOAuthInputs.set_ForwardingURL(FORWARDING_URL);

            InitializeOAuth.InitializeOAuthResultSet initializeOAuthResults = initializeOAuthChoreo.execute(initializeOAuthInputs);
            authURL = initializeOAuthResults.get_AuthorizationURL();
            callbackID = initializeOAuthResults.get_CallbackID();
            System.out.println("~~~AUTHORIZATION URL: " + authURL);
        } catch (Exception te) {
            te.printStackTrace();
        }
        return authURL;
    }

    private void generateStateToken() {
        SecureRandom random = new SecureRandom();
        stateToken = "google-" + random.nextInt();
    }

    public String getStateToken() {
        return stateToken;
    }

    public GoogleUser getUserInfo() throws IOException {
        try {
            FinalizeOAuth finalizeOAuthChoreo = new FinalizeOAuth(session);
            FinalizeOAuth.FinalizeOAuthInputSet finalizeOAuthInputs = finalizeOAuthChoreo.newInputSet();

            finalizeOAuthInputs.set_ClientID(CLIENT_ID);
            finalizeOAuthInputs.set_ClientSecret(CLIENT_SECRET);
            finalizeOAuthInputs.set_CallbackID(callbackID);

            FinalizeOAuth.FinalizeOAuthResultSet finalizeOAuthResults = finalizeOAuthChoreo.execute(finalizeOAuthInputs);
            if (finalizeOAuthResults.getException() == null) {
                GoogleUser googleUser = new GoogleUser();
                googleUser.setUserId(finalizeOAuthResults.getId());
                googleUser.setAccessToken(finalizeOAuthResults.get_AccessToken());
                googleUser.setRefreshToken(finalizeOAuthResults.get_RefreshToken());
                finalizeOAuthResults.getOutputs();
                return googleUser;
            }
        } catch (Exception te) {
            te.printStackTrace();
        }
        return null;
    }

    private List<Pair<String, String>> getParents(String path, GoogleUser user) throws TembooException, JSONException {
        com.temboo.Library.Google.Drive.Parents.List listChoreo = new com.temboo.Library.Google.Drive.Parents.List(session);
        com.temboo.Library.Google.Drive.Parents.List.ListInputSet listInputs = listChoreo.newInputSet();

        listInputs.set_ClientID(CLIENT_ID);
        listInputs.set_ClientSecret(CLIENT_SECRET);
        listInputs.set_AccessToken(user.getAccessToken());
        listInputs.set_FileID(path);
// Execute Choreo
        com.temboo.Library.Google.Drive.Parents.List.ListResultSet listResults = listChoreo.execute(listInputs);
        JSONObject jsonObject = new JSONObject(listResults.get_Response());
        JSONArray array = jsonObject.getJSONArray("items");
        List<Pair<String, String>> pairs = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.getJSONObject(i);
            if (folders.containsKey(item.getString("id"))) {
                pairs.add(new Pair<>(item.getString("id"), folders.get(item.getString("id"))));
            } else if (item.getBoolean("isRoot")) {
                pairs.add(new Pair<>("root", "root"));
            }
        }
        return pairs;
    }

    public CloudFolder getFolderContent(String path, Account account) throws TembooException, JSONException, UnsupportedEncodingException {
        GoogleUser user = account.getGoogleUser();
        com.temboo.Library.Google.Drive.Files.List filesListChoreo = new com.temboo.Library.Google.Drive.Files.List(session);
        com.temboo.Library.Google.Drive.Files.List.ListInputSet filesListInputs = filesListChoreo.newInputSet();

        filesListInputs.set_ClientID(CLIENT_ID);
        filesListInputs.set_ClientSecret(CLIENT_SECRET);
        filesListInputs.set_AccessToken(user.getAccessToken());
        filesListInputs.setInput("corpus", "domain");
        filesListInputs.set_MaxResults(1000);
        filesListInputs.set_Query("'" + path + "' in parents");

        com.temboo.Library.Google.Drive.Files.List.ListResultSet filesListResults = filesListChoreo.execute(filesListInputs);
        if (filesListResults.getException() == null) {
            JSONObject result = new JSONObject(filesListResults.get_Response());
            CloudFolder cloudFolder = new CloudFolder();
            cloudFolder.setCurrentPath(path);
            cloudFolder.setShowName(path);
            cloudFolder.setPath(getParents(path, user));

            JSONArray content = result.getJSONArray("items");
            List<CloudFile> items = new ArrayList<>();
            List<CloudFile> folders = new ArrayList<>();
            List<CloudFile> files = new ArrayList<>();
            for (int i = 0; i < content.length(); i++) {
                CloudFile cloudFile = new CloudFile();
                JSONObject object = content.getJSONObject(i);
                cloudFile.setPath(object.getString("id"));
                cloudFile.setShowName(object.getString("title"));
                cloudFile.setDir(object.getString("mimeType").equals(MIME_TYPE_FOLDER) ? true : false);
                /*if(!cloudFile.isDir())
                    cloudFile.setBytes((int) object.getLong("fileSize"));*/
//                cloudFile.setRoot(object.getString("root"));
                if (cloudFile.isDir()) {
                    folders.add(cloudFile);
                    this.folders.put(cloudFile.getPath(), cloudFile.getShowName());
                } else {
                    cloudFile.setFileType(object.getString("title").substring(object.getString("title").lastIndexOf(".") + 1));
                    files.add(cloudFile);
                }
            }
            cloudFolder.setContent(items);
            cloudFolder.setFiles(files);
            cloudFolder.setFolders(folders);
            return cloudFolder;
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
            com.temboo.Library.Google.Drive.Files.List.ListResultSet filesListResults = filesListChoreo.execute(filesListInputs);
            if (filesListResults.getException() == null) {
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
        }
        return folderList;
    }

    public String getDownloadFileLink(String path, Account account) throws TembooException, JSONException {
        GoogleUser user = account.getGoogleUser();
        Get getChoreo = new Get(session);
        Get.GetInputSet getInputs = getChoreo.newInputSet();

        getInputs.set_ClientID(CLIENT_ID);
        getInputs.set_ClientSecret(CLIENT_SECRET);
        getInputs.set_AccessToken(user.getAccessToken());
        getInputs.set_FileID(path);
        getInputs.setInput("alt", "media");

        Get.GetResultSet getResults = getChoreo.execute(getInputs);
        JSONObject result = new JSONObject(getResults.get_FileMetadata());
        if (result.has("exportLinks")) {
            JSONObject object = result.getJSONObject("exportLinks");
            return object.getString("application/zip");
        }
        return result.getString("webContentLink");
    }

    public boolean delete(String path, Account account) throws TembooException, JSONException {
        GoogleUser user = account.getGoogleUser();
        Delete deleteChoreo = new Delete(session);
        Delete.DeleteInputSet deleteInputs = deleteChoreo.newInputSet();

        deleteInputs.set_ClientID(CLIENT_ID);
        deleteInputs.set_ClientSecret(CLIENT_SECRET);
        deleteInputs.set_AccessToken(user.getAccessToken());
        deleteInputs.set_FileID(path);

        Delete.DeleteResultSet deleteResults = deleteChoreo.execute(deleteInputs);

        return "SUCCESS".equals(deleteResults.getCompletionStatus());
    }

    public boolean createFolder(String path, String folderName, Account account) throws TembooException, JSONException {
        GoogleUser user = account.getGoogleUser();
        Insert insertChoreo = new Insert(session);
        Insert.InsertInputSet insertInputs = insertChoreo.newInputSet();

        insertInputs.set_ClientID(CLIENT_ID);
        insertInputs.set_ClientSecret(CLIENT_SECRET);
        insertInputs.set_AccessToken(user.getAccessToken());

        JSONObject object = new JSONObject();
        object.put("title", folderName);
        object.put("mimeType", MIME_TYPE_FOLDER);
        if (!path.equals("") && !path.equals("root")) {
            JSONObject parents = new JSONObject();
            parents.put("id", path);
            object.append("parents", parents);
        }
        insertInputs.set_RequestBody(object.toString());
        Insert.InsertResultSet insertResults = insertChoreo.execute(insertInputs);
        return "SUCCESS".equals(insertResults.getCompletionStatus());

    }

    public boolean uploadFile(MultipartFile file, String path, Account account) throws TembooException, IOException, JSONException {
        GoogleUser user = account.getGoogleUser();
        Insert insertChoreo = new Insert(session);
        Insert.InsertInputSet insertInputs = insertChoreo.newInputSet();

        insertInputs.set_ClientID(CLIENT_ID);
        insertInputs.set_ClientSecret(CLIENT_SECRET);
        insertInputs.set_AccessToken(user.getAccessToken());

        JSONObject object = new JSONObject();
        object.put("title", file.getOriginalFilename());
        if (!path.equals("") && !path.equals("root")) {
            JSONObject parents = new JSONObject();
            parents.put("id", path);
            object.append("parents", parents);
        }
        insertInputs.set_RequestBody(object.toString());
        String base64 = Base64.encode(file.getBytes());
        insertInputs.set_FileContent(base64);
        if (file.getContentType() != null)
            insertInputs.set_ContentType(file.getContentType());
        else
            insertInputs.set_ContentType("text/plain");
        try {
            Insert.InsertResultSet insertResults = insertChoreo.execute(insertInputs);
            return "SUCCESS".equals(insertResults.getCompletionStatus());
        } catch (Exception ex) {
            System.out.println("err");
        }
        return true;
    }

}
