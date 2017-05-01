package by.fpm.barbuk.dropbox;

import by.fpm.barbuk.account.Account;
import by.fpm.barbuk.account.CloudUser;
import by.fpm.barbuk.cloudEntities.CloudFile;
import by.fpm.barbuk.cloudEntities.CloudFolder;
import by.fpm.barbuk.cloudEntities.FolderList;
import by.fpm.barbuk.temboo.CloudHelper;
import by.fpm.barbuk.temboo.TembooHelper;
import com.dropbox.core.*;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.*;
import com.sun.org.apache.xml.internal.security.utils.Base64;
import com.temboo.Library.Dropbox.Account.AccountInfo;
import com.temboo.Library.Dropbox.FilesAndMetadata.ListFolderContents;
import com.temboo.Library.Dropbox.FilesAndMetadata.UploadFile;
import com.temboo.core.TembooException;
import javafx.util.Pair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;


public final class DropboxHelper extends TembooHelper implements CloudHelper {

    // Provide your dropbox App ID and App Secret.
    private static final String APP_ID = "mvfavfx9yg4ts62";
    private static final String APP_SECRET = "2g8wfrnbdpuc1of";

    // Callback URI that Temboo will redirect to after successful authentication.
    private static final String FORWARDING_URL = "http://localhost:8080/dropbox/OAuthLogIn";
    DbxAppInfo appInfo = new DbxAppInfo(APP_ID, APP_SECRET);
    DbxRequestConfig config = new DbxRequestConfig("JavaTutorial/1.0", "utf-8");
    DbxWebAuth webAuth = new DbxWebAuth(config, appInfo);
    String sessionKey = "dropbox-auth-csrf-token";
    DbxSessionStore csrfTokenStore;
    DbxClientV2 client;

    public String getLoginUrl(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        csrfTokenStore = new DbxStandardSessionStore(session, sessionKey);
        DbxWebAuth.Request webAuthRequest = DbxWebAuth.newRequestBuilder()
                .withRedirectUri(FORWARDING_URL, csrfTokenStore)
                .build();
        return webAuth.authorize(webAuthRequest);
    }


    @Override
    public String getLoginUrl() {
        return null;
    }

    @Override
    public String getStateToken() {
        return stateToken;
    }

    @Override
    public CloudUser getUserInfo() throws IOException {
        return null;
    }

    public DropboxUser getUserInfo(HttpServletRequest request) throws IOException {
        try {
            DbxAuthFinish authFinish = webAuth.finishFromRedirect(FORWARDING_URL, csrfTokenStore, request.getParameterMap());
            client = new DbxClientV2(config, authFinish.getAccessToken());
            DropboxUser dropboxUser = new DropboxUser();
            dropboxUser.setAccessToken(authFinish.getAccessToken());
            dropboxUser.setUserId(authFinish.getUserId());
            return dropboxUser;
        } catch (DbxException e) {
            e.printStackTrace();
        } catch (DbxWebAuth.BadRequestException e) {
            e.printStackTrace();
        } catch (DbxWebAuth.ProviderException e) {
            e.printStackTrace();
        } catch (DbxWebAuth.NotApprovedException e) {
            e.printStackTrace();
        } catch (DbxWebAuth.CsrfException e) {
            e.printStackTrace();
        } catch (DbxWebAuth.BadStateException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public CloudFolder getFolderContent(String path, Account account) throws TembooException, JSONException, UnsupportedEncodingException {
        DropboxUser user = account.getDropboxUser();
        try {
            ListFolderResult listResult = client.files().listFolder("root".equals(path) ? "" : path);
            CloudFolder cloudFolder = new CloudFolder();

            List<Pair<String, String>> folderPathList = new ArrayList<>();
            String[] folderPath = path.split("/");
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
            cloudFolder.setCurrentPath(path);
            cloudFolder.setSize("0");
            cloudFolder.setDir(true);
            cloudFolder.setBytes(0);
            List<CloudFile> items = new ArrayList<>();
            List<CloudFile> folders = new ArrayList<>();
            List<CloudFile> files = new ArrayList<>();
            while (true) {
                for (Metadata metadata : listResult.getEntries()) {
                    if (metadata instanceof FileMetadata) {
                        FileMetadata fileMetadata = (FileMetadata) metadata;
                        CloudFile cloudFile = new CloudFile();
                        cloudFile.setRev(fileMetadata.getRev());
                        cloudFile.setPath(fileMetadata.getPathDisplay());
                        cloudFile.setShowName(fileMetadata.getName());
                        cloudFile.setSize(String.valueOf(fileMetadata.getSize()));
                        cloudFile.setDir(false);
                        cloudFile.setBytes((int) fileMetadata.getSize());
                        cloudFile.setRoot(path);
                        items.add(cloudFile);
                        cloudFile.setFileType(cloudFile.getPath().substring(cloudFile.getPath().lastIndexOf(".") + 1));
                        files.add(cloudFile);

                    } else if (metadata instanceof FolderMetadata) {
                        FolderMetadata folderMetadata = (FolderMetadata) metadata;
                        CloudFile cloudFile = new CloudFile();
                        cloudFile.setPath(folderMetadata.getPathDisplay());
                        cloudFile.setShowName(folderMetadata.getName());
                        cloudFile.setDir(true);
                        cloudFile.setRoot(path);
                        items.add(cloudFile);
                        folders.add(cloudFile);
                    }
                }

                if (!listResult.getHasMore()) {
                    break;
                }

                listResult = client.files().listFolderContinue(listResult.getCursor());
            }
            cloudFolder.setContent(items);
            cloudFolder.setFiles(files);
            cloudFolder.setFolders(folders);
            return cloudFolder;
        } catch (ListFolderErrorException e) {
            e.printStackTrace();
        } catch (ListFolderContinueErrorException e) {
            e.printStackTrace();
        } catch (DbxException e) {
            e.printStackTrace();
        }
        return new CloudFolder();

    }


    @Override
    public FolderList getFolders(String path, Account account) throws
            TembooException, JSONException, UnsupportedEncodingException {
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
    public String getDownloadFileLink(String path, Account account, boolean isFileContent) throws TembooException {
        DropboxUser user = account.getDropboxUser();
        try {
            GetTemporaryLinkResult result = client.files().getTemporaryLink(path);
            return result.getLink();
        } catch (DbxException e) {
            e.printStackTrace();
        }
        return "";
    }

    @Override
    public boolean delete(String path, Account account) throws TembooException, JSONException {
        try {
            client.files().delete(path);
            return true;
        } catch (DbxException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean createFolder(String path, Account account) throws TembooException {
        try {
            client.files().createFolder(path);
        } catch (DbxException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public String uploadFile(MultipartFile file, String path, Account account) throws TembooException, IOException {
        DropboxUser user = account.getDropboxUser();
        try {
            client.files().upload(("root".equals(path) ? "" : path) + "/" + file.getOriginalFilename()).uploadAndFinish(file.getInputStream());
            return ("root".equals(path) ? "" : path) + "/" + file.getOriginalFilename();
        } catch (DbxException e) {
            e.printStackTrace();
        }
        return "";
    }

    @Override
    public long getAvailableSize(Account account) throws TembooException, JSONException {
        DropboxUser user = account.getDropboxUser();
        AccountInfo accountInfo = new AccountInfo(session);
        AccountInfo.AccountInfoInputSet inputSet = accountInfo.newInputSet();
        inputSet.set_AppKey(APP_ID);
        inputSet.set_AppSecret(APP_SECRET);
        inputSet.set_AccessToken(user.getAccessToken());
        inputSet.set_AccessTokenSecret(user.getAccessSecret());

        /*AccountInfo.AccountInfoResultSet resultSet = accountInfo.execute(inputSet);
        if (resultSet.getException() == null) {
            JSONObject result = new JSONObject(resultSet.get_Response());
            long size = result.getJSONObject("quota_info").getLong("quota")-result.getJSONObject("quota_info").getLong("normal")-result.getJSONObject("quota_info").getLong("shared");
            if(size>0)
                return size;
        }*/
        return 0;
    }

    public Account encrypt(String path, Account account) throws TembooException, IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, JSONException {
        DropboxUser dropboxUser = account.getDropboxUser();
        try {


            String urlStr = getDownloadFileLink(path, account, false);
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
