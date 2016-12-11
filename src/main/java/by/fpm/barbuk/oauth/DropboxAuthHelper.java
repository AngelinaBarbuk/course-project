package by.fpm.barbuk.oauth;

import by.fpm.barbuk.dropbox.CloudFile;
import by.fpm.barbuk.dropbox.CloudFolder;
import by.fpm.barbuk.dropbox.DropboxUser;
import com.temboo.Library.Dropbox.FileOperations.DeleteFileOrFolder;
import com.temboo.Library.Dropbox.FilesAndMetadata.GetDownloadLink;
import com.temboo.Library.Dropbox.FilesAndMetadata.GetShareableLink;
import com.temboo.Library.Dropbox.FilesAndMetadata.ListFolderContents;
import com.temboo.Library.Dropbox.OAuth.FinalizeOAuth;
import com.temboo.Library.Dropbox.OAuth.InitializeOAuth;
import com.temboo.core.TembooException;
import com.temboo.core.TembooSession;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public final class DropboxAuthHelper {

    // Provide your dropbox App ID and App Secret.
    private static final String APP_ID = "mvfavfx9yg4ts62";
    private static final String APP_SECRET = "2g8wfrnbdpuc1of";

    // Callback URI that Temboo will redirect to after successful authentication.
    private static final String FORWARDING_URL = "http://localhost:8080/dropbox/OAuthLogIn";

    private String stateToken;

    // Replace with your Temboo credentials.
    private static final String TEMBOO_ACCOUNT_NAME = "angelinabarbuk";
    private static final String TEMBOO_APP_KEY_NAME = "myFirstApp";
    private static final String TEMBOO_APP_KEY_VALUE = "dz7TFlG60iXUQhhBkR4bBZ7jMQyK7U0A";

    private TembooSession session = null;

    private String tokenSecret = "";
    private String callbackID = "";

    public DropboxAuthHelper() {

        generateStateToken();
        try {
            session = new TembooSession(TEMBOO_ACCOUNT_NAME, TEMBOO_APP_KEY_NAME, TEMBOO_APP_KEY_VALUE);
        } catch (Exception te) {
            te.printStackTrace();
        }
    }

    public String getLoginUrl() {

        String authURL = "";
        try {
            InitializeOAuth initializeOAuthChoreo = new InitializeOAuth(session);
            InitializeOAuth.InitializeOAuthInputSet initializeOAuthInputs = initializeOAuthChoreo.newInputSet();
            initializeOAuthInputs.setCredential("course");
            initializeOAuthInputs.set_ForwardingURL(FORWARDING_URL);
            InitializeOAuth.InitializeOAuthResultSet initializeOAuthResults = initializeOAuthChoreo.execute(initializeOAuthInputs);
            authURL = initializeOAuthResults.get_AuthorizationURL();
            tokenSecret = initializeOAuthResults.get_OAuthTokenSecret();
            callbackID = initializeOAuthResults.get_CallbackID();
            System.out.println("~~~AUTHORIZATION URL: " + authURL);
        } catch (Exception te) {
            te.printStackTrace();
        }
        return authURL;
    }

    private void generateStateToken() {
        SecureRandom random = new SecureRandom();
        stateToken = "dropbox-" + random.nextInt();
    }

    public String getStateToken() {
        return stateToken;
    }

    public DropboxUser getUserInfo() throws IOException {
        try {
            FinalizeOAuth finalizeOAuthChoreo = new FinalizeOAuth(session);
            FinalizeOAuth.FinalizeOAuthInputSet finalizeOAuthInputs = finalizeOAuthChoreo.newInputSet();

            finalizeOAuthInputs.set_OAuthTokenSecret(tokenSecret);
            finalizeOAuthInputs.set_DropboxAppSecret(APP_SECRET);
            finalizeOAuthInputs.set_DropboxAppKey(APP_ID);
            finalizeOAuthInputs.set_CallbackID(callbackID);

            FinalizeOAuth.FinalizeOAuthResultSet finalizeOAuthResults = finalizeOAuthChoreo.execute(finalizeOAuthInputs);
            if (finalizeOAuthResults.getException() == null) {
                DropboxUser dropboxUser = new DropboxUser();
                dropboxUser.setUserId(finalizeOAuthResults.get_UserID());
                dropboxUser.setAccessToken(finalizeOAuthResults.get_AccessToken());
                dropboxUser.setAccessSecret(finalizeOAuthResults.get_AccessTokenSecret());
                finalizeOAuthResults.getOutputs();
                return dropboxUser;
            }
        } catch (Exception te) {
            te.printStackTrace();
        }
        return null;
    }

    public CloudFolder getFolderContent(String path, DropboxUser user) throws TembooException, JSONException, UnsupportedEncodingException {
        ListFolderContents listFolderContentsChoreo = new ListFolderContents(session);
        ListFolderContents.ListFolderContentsInputSet listFolderContentsInputs = listFolderContentsChoreo.newInputSet();

        listFolderContentsInputs.set_AppKey(APP_ID);
        listFolderContentsInputs.set_AppSecret(APP_SECRET);
        listFolderContentsInputs.set_AccessToken(user.getAccessToken());
        listFolderContentsInputs.set_AccessTokenSecret(user.getAccessSecret());
        listFolderContentsInputs.set_Folder("root".equals(path) ? "" : path);

        ListFolderContents.ListFolderContentsResultSet listFolderContentsResults = listFolderContentsChoreo.execute(listFolderContentsInputs);
        if (listFolderContentsResults.getException() == null) {
            JSONObject result = new JSONObject(listFolderContentsResults.get_Response());
            CloudFolder cloudFolder = new CloudFolder();
            cloudFolder.setPath(result.getString("path"));
            cloudFolder.setShowName(cloudFolder.getPath().substring(cloudFolder.getPath().lastIndexOf("/")+1));
//            cloudFolder.setPath(new String(result.getString("path").getBytes("windows-1251"), "UTF-8"));
            cloudFolder.setSize(result.getString("size"));
            cloudFolder.setDir(result.getBoolean("is_dir"));
            cloudFolder.setBytes(result.getInt("bytes"));
            cloudFolder.setRoot(result.getString("root"));
            JSONArray content = result.getJSONArray("contents");
            List<CloudFile> items = new ArrayList<>();
            List<CloudFile> folders = new ArrayList<>();
            List<CloudFile> files = new ArrayList<>();
            for (int i = 0; i < content.length(); i++) {
                CloudFile cloudFile = new CloudFile();
                JSONObject object = content.getJSONObject(i);
                cloudFile.setRev(object.getString("rev"));
                cloudFile.setPath(object.getString("path"));
                cloudFile.setShowName(cloudFile.getPath().substring(cloudFile.getPath().lastIndexOf("/")+1));
//                cloudFile.setPath(new String(object.getString("path").getBytes("windows-1251"), "UTF-8"));
                cloudFile.setSize(object.getString("size"));
                cloudFile.setReadOnly(object.getBoolean("read_only"));
                cloudFile.setDir(object.getBoolean("is_dir"));
                cloudFile.setBytes(object.getInt("bytes"));
                cloudFile.setRoot(object.getString("root"));
                items.add(cloudFile);
                if (cloudFile.isDir())
                    folders.add(cloudFile);
                else {
                    cloudFile.setFileType(cloudFile.getPath().substring(cloudFile.getPath().lastIndexOf(".") + 1));
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

    public String getDownloadFileLink(String path, DropboxUser user) throws TembooException {

        GetDownloadLink getDownloadLinkChoreo = new GetDownloadLink(session);
        GetDownloadLink.GetDownloadLinkInputSet getDownloadLinkInputs = getDownloadLinkChoreo.newInputSet();

        getDownloadLinkInputs.set_AppKey(APP_ID);
        getDownloadLinkInputs.set_AppSecret(APP_SECRET);
        getDownloadLinkInputs.set_AccessToken(user.getAccessToken());
        getDownloadLinkInputs.set_AccessTokenSecret(user.getAccessSecret());
        getDownloadLinkInputs.set_Path(path);

        GetDownloadLink.GetDownloadLinkResultSet getDownloadLinkResults = getDownloadLinkChoreo.execute(getDownloadLinkInputs);
        return getDownloadLinkResults.get_URL();

/*        GetShareableLink getShareableLinkChoreo = new GetShareableLink(session);

// Get an InputSet object for the choreo
        GetShareableLink.GetShareableLinkInputSet getShareableLinkInputs = getShareableLinkChoreo.newInputSet();

        getShareableLinkInputs.set_AppKey(APP_ID);
        getShareableLinkInputs.set_AppSecret(APP_SECRET);
        getShareableLinkInputs.set_AccessToken(user.getAccessToken());
        getShareableLinkInputs.set_AccessTokenSecret(user.getAccessSecret());
        getShareableLinkInputs.set_Path(path);


// Execute Choreo
        GetShareableLink.GetShareableLinkResultSet getShareableLinkResults = getShareableLinkChoreo.execute(getShareableLinkInputs);
        return getShareableLinkResults.get_Response();*/
    }

    public JSONObject delete(String path, DropboxUser user) throws TembooException, JSONException {

        DeleteFileOrFolder deleteFileOrFolderChoreo = new DeleteFileOrFolder(session);
        DeleteFileOrFolder.DeleteFileOrFolderInputSet deleteFileOrFolderInputs = deleteFileOrFolderChoreo.newInputSet();

        deleteFileOrFolderInputs.set_AppKey(APP_ID);
        deleteFileOrFolderInputs.set_AppSecret(APP_SECRET);
        deleteFileOrFolderInputs.set_AccessToken(user.getAccessToken());
        deleteFileOrFolderInputs.set_AccessTokenSecret(user.getAccessSecret());
        deleteFileOrFolderInputs.set_Path(path);

        DeleteFileOrFolder.DeleteFileOrFolderResultSet deleteFileOrFolderResults = deleteFileOrFolderChoreo.execute(deleteFileOrFolderInputs);
        JSONObject result = new JSONObject(deleteFileOrFolderResults.get_Response());
        return result;
    }
}