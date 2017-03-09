package by.fpm.barbuk.dropbox;

import by.fpm.barbuk.account.Account;
import by.fpm.barbuk.cloudEntities.CloudFile;
import by.fpm.barbuk.cloudEntities.CloudFolder;
import by.fpm.barbuk.cloudEntities.FolderList;
import by.fpm.barbuk.temboo.CloudHelper;
import by.fpm.barbuk.temboo.TembooHelper;
import com.sun.org.apache.xml.internal.security.utils.Base64;
import com.temboo.Library.Dropbox.FileOperations.CreateFolder;
import com.temboo.Library.Dropbox.FileOperations.DeleteFileOrFolder;
import com.temboo.Library.Dropbox.FilesAndMetadata.GetDownloadLink;
import com.temboo.Library.Dropbox.FilesAndMetadata.ListFolderContents;
import com.temboo.Library.Dropbox.FilesAndMetadata.UploadFile;
import com.temboo.Library.Dropbox.OAuth.FinalizeOAuth;
import com.temboo.Library.Dropbox.OAuth.InitializeOAuth;
import com.temboo.core.TembooException;
import javafx.util.Pair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public final class DropboxHelper extends TembooHelper implements CloudHelper {

    // Provide your dropbox App ID and App Secret.
    private static final String APP_ID = "mvfavfx9yg4ts62";
    private static final String APP_SECRET = "2g8wfrnbdpuc1of";

    // Callback URI that Temboo will redirect to after successful authentication.
    private static final String FORWARDING_URL = "http://localhost:8080/dropbox/OAuthLogIn";


    // Replace with your Temboo credentials.
    /*private static final String TEMBOO_ACCOUNT_NAME = "angelinabarbukyandex";
    private static final String TEMBOO_APP_KEY_NAME = "myFirstApp";
    private static final String TEMBOO_APP_KEY_VALUE = "uNiSBe4QkSlOtZ1a7Zozh4sERQfQOdQh";

    private TembooSession session = null;

    private String tokenSecret = "";
    private String callbackID = "";

    public DropboxHelper() {

        generateStateToken();
        try {
            session = new TembooSession(TEMBOO_ACCOUNT_NAME, TEMBOO_APP_KEY_NAME, TEMBOO_APP_KEY_VALUE);
        } catch (Exception te) {
            te.printStackTrace();
        }
    }*/

    @Override
    public String getLoginUrl() {

        String authURL = "";
        try {
            InitializeOAuth initializeOAuthChoreo = new InitializeOAuth(session);
            InitializeOAuth.InitializeOAuthInputSet initializeOAuthInputs = initializeOAuthChoreo.newInputSet();
            initializeOAuthInputs.set_DropboxAppSecret(APP_SECRET);
            initializeOAuthInputs.set_DropboxAppKey(APP_ID);
            /*initializeOAuthInputs.setCredential("course");*/
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


    @Override
    public String getStateToken() {
        return stateToken;
    }

    @Override
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
                dropboxUser.setEncryptionKeys(new HashMap<>());
                return dropboxUser;
            }
        } catch (Exception te) {
            te.printStackTrace();
        }
        return null;
    }

    @Override
    public CloudFolder getFolderContent(String path, Account account) throws TembooException, JSONException, UnsupportedEncodingException {
        DropboxUser user = account.getDropboxUser();
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

            List<Pair<String, String>> folderPathList = new ArrayList<>();
            String[] folderPath = result.getString("path").split("/");
            if (folderPath.length >= 2) {
                folderPathList.add(new Pair("root", "root"));
            }
            for (int i = 2; i < folderPath.length; i++) {
                StringBuffer sb = new StringBuffer();
                for (int j = 1; j < i; j++)
                    sb.append("/" + folderPath[j]);
                folderPathList.add(new Pair(sb.toString(), folderPath[i - 1]));
            }
            cloudFolder.setPath(folderPathList);
            cloudFolder.setCurrentPath(result.getString("path"));
            cloudFolder.setShowName(result.getString("path").substring(cloudFolder.getPath().lastIndexOf("/") + 1));
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
                cloudFile.setShowName(cloudFile.getPath().substring(cloudFile.getPath().lastIndexOf("/") + 1));
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


    @Override
    public FolderList getFolders(String path, Account account) throws TembooException, JSONException, UnsupportedEncodingException {
        DropboxUser user = account.getDropboxUser();
        FolderList folderList = new FolderList();
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
            JSONArray content = result.getJSONArray("contents");
            List<CloudFile> folders = new ArrayList<>();
            for (int i = 0; i < content.length(); i++) {
                CloudFile cloudFile = new CloudFile();
                JSONObject object = content.getJSONObject(i);
                cloudFile.setDir(object.getBoolean("is_dir"));
                if (cloudFile.isDir()) {
                    cloudFile.setRev(object.getString("rev"));
                    cloudFile.setPath(object.getString("path"));
                    cloudFile.setShowName(cloudFile.getPath().substring(cloudFile.getPath().lastIndexOf("/") + 1));
                    cloudFile.setSize(object.getString("size"));
                    cloudFile.setReadOnly(object.getBoolean("read_only"));
                    cloudFile.setBytes(object.getInt("bytes"));
                    cloudFile.setRoot(object.getString("root"));
                    folders.add(cloudFile);
                }
            }
            folderList.setFolders(folders);
            if ("root".equals(path)) {
                folderList.setPrevFolder("");
            } else {
                String prev = path.substring(0, path.lastIndexOf("/"));
                if ("".equals(prev))
                    folderList.setPrevFolder("root");
                else
                    folderList.setPrevFolder(prev);
            }
            return folderList;
        }
        return folderList;
    }

    @Override
    public String getDownloadFileLink(String path, Account account) throws TembooException {
        DropboxUser user = account.getDropboxUser();
        GetDownloadLink getDownloadLinkChoreo = new GetDownloadLink(session);
        GetDownloadLink.GetDownloadLinkInputSet getDownloadLinkInputs = getDownloadLinkChoreo.newInputSet();

        getDownloadLinkInputs.set_AppKey(APP_ID);
        getDownloadLinkInputs.set_AppSecret(APP_SECRET);
        getDownloadLinkInputs.set_AccessToken(user.getAccessToken());
        getDownloadLinkInputs.set_AccessTokenSecret(user.getAccessSecret());
        getDownloadLinkInputs.set_Path(path);

        GetDownloadLink.GetDownloadLinkResultSet getDownloadLinkResults = getDownloadLinkChoreo.execute(getDownloadLinkInputs);
        return getDownloadLinkResults.get_URL();
    }

    @Override
    public boolean delete(String path, Account account) throws TembooException, JSONException {
        DropboxUser user = account.getDropboxUser();
        DeleteFileOrFolder deleteFileOrFolderChoreo = new DeleteFileOrFolder(session);
        DeleteFileOrFolder.DeleteFileOrFolderInputSet deleteFileOrFolderInputs = deleteFileOrFolderChoreo.newInputSet();

        deleteFileOrFolderInputs.set_AppKey(APP_ID);
        deleteFileOrFolderInputs.set_AppSecret(APP_SECRET);
        deleteFileOrFolderInputs.set_AccessToken(user.getAccessToken());
        deleteFileOrFolderInputs.set_AccessTokenSecret(user.getAccessSecret());
        deleteFileOrFolderInputs.set_Path(path);

        DeleteFileOrFolder.DeleteFileOrFolderResultSet deleteFileOrFolderResults = deleteFileOrFolderChoreo.execute(deleteFileOrFolderInputs);
        JSONObject result = new JSONObject(deleteFileOrFolderResults.get_Response());
        return result.getBoolean("is_deleted");
    }

    public boolean createFolder(String path, Account account) throws TembooException {
        DropboxUser user = account.getDropboxUser();
        CreateFolder createFolderChoreo = new CreateFolder(session);
        CreateFolder.CreateFolderInputSet createFolderInputs = createFolderChoreo.newInputSet();

        createFolderInputs.set_AppKey(APP_ID);
        createFolderInputs.set_AppSecret(APP_SECRET);
        createFolderInputs.set_AccessToken(user.getAccessToken());
        createFolderInputs.set_AccessTokenSecret(user.getAccessSecret());
        createFolderInputs.set_NewFolderName(path);

        CreateFolder.CreateFolderResultSet createFolderResults = createFolderChoreo.execute(createFolderInputs);
        return true;
    }

    @Override
    public boolean uploadFile(MultipartFile file, String path, Account account) throws TembooException, IOException {
        DropboxUser user = account.getDropboxUser();
        UploadFile uploadFileChoreo = new UploadFile(session);
        UploadFile.UploadFileInputSet uploadFileInputs = uploadFileChoreo.newInputSet();

        uploadFileInputs.set_AppKey(APP_ID);
        uploadFileInputs.set_AppSecret(APP_SECRET);
        uploadFileInputs.set_AccessToken(user.getAccessToken());
        uploadFileInputs.set_AccessTokenSecret(user.getAccessSecret());
        uploadFileInputs.set_Folder("root".equals(path) ? "" : path);
        String base64 = Base64.encode(file.getBytes());
        uploadFileInputs.set_FileContents(base64);
        uploadFileInputs.set_FileName(file.getOriginalFilename());

        UploadFile.UploadFileResultSet uploadFileResults = uploadFileChoreo.execute(uploadFileInputs);
        return true;
    }

    public Account encrypt(String path, Account account) throws TembooException, IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, JSONException {
        DropboxUser dropboxUser = account.getDropboxUser();
        try {


            String urlStr = getDownloadFileLink(path, account);
            URL url = new URL(urlStr);
            HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
            int responseCode = httpConn.getResponseCode();

            // always check HTTP response code first
            if (responseCode == HttpURLConnection.HTTP_OK) {
                String fileName = "";
                String disposition = httpConn.getHeaderField("Content-Disposition");
                String contentType = httpConn.getContentType();
                int contentLength = httpConn.getContentLength();

                if (disposition != null) {
                    int index = disposition.indexOf("filename=");
                    if (index > 0) {
                        fileName = disposition.substring(index + 10,
                                disposition.length() - 1);
                        fileName = fileName.substring(0, fileName.indexOf("\""));
                    }
                } else {
                    fileName = urlStr.substring(urlStr.lastIndexOf("/") + 1,
                            urlStr.length());
                }

                System.out.println("Content-Type = " + contentType);
                System.out.println("Content-Disposition = " + disposition);
                System.out.println("Content-Length = " + contentLength);
                System.out.println("fileName = " + fileName);

                // opens input stream from the HTTP connection
                InputStream inputStream = httpConn.getInputStream();
                MultipartFile multipartFile = new MockMultipartFile(fileName, fileName, contentType, inputStream);
                inputStream.close();
                httpConn.disconnect();

                UploadFile uploadFileChoreo = new UploadFile(session);
                UploadFile.UploadFileInputSet uploadFileInputs = uploadFileChoreo.newInputSet();

                uploadFileInputs.set_AppKey(APP_ID);
                uploadFileInputs.set_AppSecret(APP_SECRET);
                uploadFileInputs.set_AccessToken(dropboxUser.getAccessToken());
                uploadFileInputs.set_AccessTokenSecret(dropboxUser.getAccessSecret());
                uploadFileInputs.set_Folder("root".equals(path) ? "" : path);
                if (!dropboxUser.getEncryptionKeys().containsKey(path)) {
                    KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                    SecureRandom random = new SecureRandom();
                    keyGen.init(random);
                    SecretKey secretKey = keyGen.generateKey();
                    Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
                    cipher.init(Cipher.ENCRYPT_MODE, secretKey);
                    byte[] encrypted = cipher.doFinal(multipartFile.getBytes());
                    String base64 = Base64.encode(encrypted);
                    uploadFileInputs.set_FileContents(base64);
                    uploadFileInputs.set_FileName(fileName);
                    UploadFile.UploadFileResultSet uploadFileResults = uploadFileChoreo.execute(uploadFileInputs);
                    JSONObject response = new JSONObject(uploadFileResults.get_Response());
                    dropboxUser.getEncryptionKeys().put(response.getString("path"), secretKey);

                } else {
                    SecretKey secretKey = dropboxUser.getEncryptionKeys().get(path);
                    Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
                    cipher.init(Cipher.DECRYPT_MODE, secretKey);
                    byte[] decrypted = cipher.doFinal(multipartFile.getBytes());
                    String base64 = Base64.encode(decrypted);
                    uploadFileInputs.set_FileContents(base64);
                    uploadFileInputs.set_FileName(fileName);
                    UploadFile.UploadFileResultSet uploadFileResults = uploadFileChoreo.execute(uploadFileInputs);
                }
            }
        } catch (Exception ex) {
            System.out.println("err");
        }
        return account;
    }
}
