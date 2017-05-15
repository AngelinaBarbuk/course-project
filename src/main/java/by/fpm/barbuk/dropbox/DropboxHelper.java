package by.fpm.barbuk.dropbox;

import by.fpm.barbuk.account.Account;
import by.fpm.barbuk.cloudEntities.CloudFile;
import by.fpm.barbuk.cloudEntities.CloudFolder;
import by.fpm.barbuk.uploadBigFile.DownloadUploadFile;
import com.dropbox.core.*;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.*;
import com.temboo.core.TembooException;
import javafx.util.Pair;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


@Component
@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class DropboxHelper implements DownloadUploadFile {

    // Provide your dropbox App ID and App Secret.
    private static final String APP_ID = "mvfavfx9yg4ts62";
    private static final String APP_SECRET = "2g8wfrnbdpuc1of";

    private static final String FORWARDING_URL = "http://localhost:8080/dropbox/OAuthLogIn";
    private static final String sessionKey = "dropbox-auth-csrf-token";
    private static final DbxRequestConfig config = new DbxRequestConfig("JavaTutorial/1.0", "utf-8");
    private DbxAppInfo appInfo = new DbxAppInfo(APP_ID, APP_SECRET);
    private DbxWebAuth webAuth = new DbxWebAuth(config, appInfo);
    private DbxSessionStore csrfTokenStore;
    private DbxClientV2 client;

    private static final long CHUNKED_UPLOAD_CHUNK_SIZE = 8L << 20; // 8MiB

    public DropboxHelper() {

    }

    public String getLoginUrl(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        csrfTokenStore = new DbxStandardSessionStore(session, sessionKey);
        DbxWebAuth.Request webAuthRequest = DbxWebAuth.newRequestBuilder()
                .withRedirectUri(FORWARDING_URL, csrfTokenStore)
                .build();
        return webAuth.authorize(webAuthRequest);
    }

    public DropboxUser getUserInfo(HttpServletRequest request) throws IOException {
        try {
            DbxAuthFinish authFinish = webAuth.finishFromRedirect(FORWARDING_URL, csrfTokenStore, request.getParameterMap());
            setClient(new DbxClientV2(config, authFinish.getAccessToken()));
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

    public boolean authorize(Account account) {
        try {
            DropboxUser user = account.getDropboxUser();
            setClient(new DbxClientV2(config, user.getAccessToken()));
            client.users().getSpaceUsage();
            return true;
        } catch (DbxException e) {
            e.printStackTrace();
        }
        return false;
    }

    public CloudFolder getFolderContent(String path, Account account) {
        DropboxUser user = account.getDropboxUser();
        try {
            ListFolderResult listResult = client.files().listFolder(getPath(path));
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
                        cloudFile.setDir(false);
                        cloudFile.setBytes(fileMetadata.getSize());
                        cloudFile.setRoot(path);
                        items.add(cloudFile);
                        cloudFile.setFileType(cloudFile.getPath()
                                .substring(cloudFile.getPath().lastIndexOf(".") + 1));
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

    private String getPath(String path) {
        return "root".equals(path) ? "" : path;
    }

    public String getDownloadFileLink(String path, Account account) {
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
    public boolean delete(String path, Account account) {
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
    public String uploadFile(MultipartFile file, String path, Account account) {
        DropboxUser user = account.getDropboxUser();
        long size = file.getSize();
        if (size < CHUNKED_UPLOAD_CHUNK_SIZE) {
            for (int i = 0; i < CHUNKED_UPLOAD_MAX_ATTEMPTS; ++i) {
                try {
                    FileMetadata fileMetadata = client.files()
                            .uploadBuilder((getPath(path)) + "/" + file.getOriginalFilename())
                            .withMode(WriteMode.ADD)
                            .withAutorename(true)
                            .uploadAndFinish(file.getInputStream());
                    return fileMetadata.getPathLower();
                } catch (NetworkIOException ex) {
                    // network issue with Dropbox (maybe a timeout?) try again
                    continue;
                } catch (DbxException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        long uploaded = 0L;

        String sessionId = null;
        CommitInfo commitInfo = CommitInfo.newBuilder((getPath(path)) + "/" + file.getOriginalFilename())
                .withMode(WriteMode.ADD)
                .withAutorename(true)
                .build();
        UploadSessionCursor cursor = null;
        for (int i = 0; i < CHUNKED_UPLOAD_MAX_ATTEMPTS; ++i) {

            try (InputStream in = file.getInputStream()) {
                in.skip(uploaded);
                // (1) Start
                if (sessionId == null) {
                    sessionId = client.files().uploadSessionStart()
                            .uploadAndFinish(in, CHUNKED_UPLOAD_CHUNK_SIZE)
                            .getSessionId();
                    uploaded += CHUNKED_UPLOAD_CHUNK_SIZE;
                }
                cursor = new UploadSessionCursor(sessionId, uploaded);
                // (2) Append
                while ((size - uploaded) > CHUNKED_UPLOAD_CHUNK_SIZE) {
                    client.files().uploadSessionAppendV2(cursor)
                            .uploadAndFinish(in, CHUNKED_UPLOAD_CHUNK_SIZE);
                    uploaded += CHUNKED_UPLOAD_CHUNK_SIZE;
                    cursor = new UploadSessionCursor(sessionId, uploaded);
                }
                // (3) Finish
                long remaining = size - uploaded;

                FileMetadata metadata = client.files().uploadSessionFinish(cursor, commitInfo)
                        .uploadAndFinish(in, remaining);
                System.out.println(metadata.toStringMultiline());
                return metadata.getPathLower();
            } catch (RetryException ex) {
                continue;
            } catch (NetworkIOException ex) {
                // network issue with Dropbox (maybe a timeout?) try again
                continue;
            } catch (UploadSessionLookupErrorException ex) {
                if (ex.errorValue.isIncorrectOffset()) {
                    uploaded = ex.errorValue
                            .getIncorrectOffsetValue()
                            .getCorrectOffset();
                    continue;
                }
            } catch (UploadSessionFinishErrorException ex) {
                if (ex.errorValue.isLookupFailed() && ex.errorValue.getLookupFailedValue().isIncorrectOffset()) {
                    uploaded = ex.errorValue
                            .getLookupFailedValue()
                            .getIncorrectOffsetValue()
                            .getCorrectOffset();
                    continue;
                }
            } catch (DbxException ex) {
                ex.printStackTrace();

            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return "";
    }

    @Override
    public MultipartFile downloadFile(String path, Account account) {
        InputStream in = null;
        for (int i = 0; i < CHUNKED_UPLOAD_MAX_ATTEMPTS; i++) {
            try {

                Metadata metadata = client.files().getMetadataBuilder(getPath(path)).withIncludeDeleted(true).start();
                if (metadata instanceof DeletedMetadata) {
                    ListRevisionsResult revisions = client.files().listRevisions(metadata.getPathLower());
                    if(revisions.getIsDeleted()) {
                        client.files().restore(metadata.getPathLower(), revisions.getEntries().get(revisions.getEntries().size() - 1).getRev());
                    }
                }
                if (metadata != null) {
                    in = client.files().download(metadata.getPathLower()).getInputStream();
                    if (in != null) {
                        MultipartFile multipartFile = new MockMultipartFile(metadata.getName(), in);
                        return multipartFile;
                    }
                }
            } catch (NetworkIOException ex) {
                continue;
            } catch (DownloadErrorException e) {
                continue;
            } catch (DbxException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (in != null)
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
            }
        }
        return null;
    }

    public DbxClientV2 getClient() {
        return client;
    }

    public void setClient(DbxClientV2 client) {
        this.client = client;
    }
}
