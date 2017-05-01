package by.fpm.barbuk.yandex;

import by.fpm.barbuk.account.Account;
import by.fpm.barbuk.account.CloudUser;
import by.fpm.barbuk.cloudEntities.CloudFile;
import by.fpm.barbuk.cloudEntities.CloudFolder;
import by.fpm.barbuk.cloudEntities.FolderList;
import by.fpm.barbuk.temboo.CloudHelper;
import by.fpm.barbuk.temboo.TembooHelper;
import com.squareup.okhttp.OkHttpClient;
import com.temboo.core.TembooException;
import com.yandex.disk.rest.Credentials;
import com.yandex.disk.rest.ProgressListener;
import com.yandex.disk.rest.ResourcesArgs;
import com.yandex.disk.rest.RestClient;
import com.yandex.disk.rest.exceptions.ServerException;
import com.yandex.disk.rest.exceptions.ServerIOException;
import com.yandex.disk.rest.exceptions.WrongMethodException;
import com.yandex.disk.rest.json.Link;
import com.yandex.disk.rest.json.Resource;
import com.yandex.disk.rest.retrofit.CloudApi;
import javafx.util.Pair;
import org.apache.commons.io.FileUtils;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import retrofit.RestAdapter;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;


public final class YandexHelper extends TembooHelper implements CloudHelper {

    private static String CLIENT_ID = "1a9e7ca4f42f46e6b1ebbf03a7125e20";
    private static String CLIENT_SECRET = "ff71d2231c504ac6b3ec40eb2237d3b2";
    private RestClient restClient;
    private Credentials credentials;
    private OkHttpClient client;
    private String serverURL;
    private CloudApi cloudApi;
    private RestAdapter.Builder builder;

    @Override
    public String getLoginUrl() {
        return "https://oauth.yandex.ru/authorize?response_type=code&client_id=" + CLIENT_ID;
    }

    @Override
    public String getStateToken() {
        return null;
    }

    @Override
    public CloudUser getUserInfo() throws IOException {
        return null;
    }

    public YandexUser getUserInfo(String code) throws IOException {
        YandexUser yandexUser = new YandexUser();
        URL url = new URL("https://oauth.yandex.ru/token");
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        try {
            httpConn.setRequestMethod("POST");
            httpConn.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
            String userpassword = CLIENT_ID + ":" + CLIENT_SECRET;
            String encodedAuthorization = Base64.getEncoder().encodeToString(userpassword.getBytes(StandardCharsets.UTF_8));
            httpConn.setRequestProperty("Authorization", "Basic " +
                    encodedAuthorization);

            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("grant_type", "authorization_code"));
            params.add(new BasicNameValuePair("code", code));

            httpConn.setDoOutput(true);
            OutputStream os = httpConn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(os, "UTF-8"));
            writer.write(getQuery(params));
            writer.flush();
            writer.close();

            int responseCode = httpConn.getResponseCode();
            // always check HTTP response code first
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(httpConn.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                JSONObject object = new JSONObject(response.toString());
                yandexUser.setAccessToken(object.getString("access_token"));
                yandexUser.setRefreshToken(object.getString("refresh_token"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } finally {
            httpConn.disconnect();
        }
        this.credentials = new Credentials(CLIENT_ID, yandexUser.getAccessToken());
        restClient = new RestClient(credentials);

        /*this.builder = new RestAdapter.Builder().set
                .setClient(restClient)
                .setRequestInterceptor(new RequestInterceptorImpl(credentials.getHeaders()))
                .setErrorHandler(new ErrorHandlerImpl());

        this.cloudApi = builder
                .build()
                .create(CloudApi.class);*/
        return yandexUser;
    }

    @Override
    public CloudFolder getFolderContent(String path, Account account) throws TembooException, JSONException, UnsupportedEncodingException {
        YandexUser yandexUser = account.getYandexUser();
        try {
            Resource resource = restClient.getResources(new ResourcesArgs.Builder().setPath(path).build());

            CloudFolder cloudFolder = new CloudFolder();

            List<Pair<String, String>> folderPathList = new ArrayList<>();
            String[] folderPath = resource.getPath().getPath().split("/");
            if (folderPath.length >= 2) {
                folderPathList.add(new Pair("disk:/", "root"));
            }
            for (int i = 2; i < folderPath.length; i++) {
                StringBuffer sb = new StringBuffer();
                for (int j = 1; j < i; j++)
                    sb.append("/" + folderPath[j]);
                folderPathList.add(new Pair(sb.toString(), folderPath[i - 1]));
            }
            cloudFolder.setPath(folderPathList);
            cloudFolder.setCurrentPath(resource.getPath().getPrefix() + ":" + resource.getPath().getPath());
            cloudFolder.setShowName(resource.getName());
            cloudFolder.setSize(String.valueOf(resource.getSize()));
            cloudFolder.setDir("dir".equals(resource.getType()));
            cloudFolder.setBytes((int) resource.getSize());
            cloudFolder.setRoot(resource.getPath().getPrefix());

            List<CloudFile> items = new ArrayList<>();
            List<CloudFile> folders = new ArrayList<>();
            List<CloudFile> files = new ArrayList<>();
            for (Resource res : resource.getResourceList().getItems()) {
                CloudFile cloudFile = new CloudFile();
                cloudFile.setPath(res.getPath().getPrefix() + ":" + res.getPath().getPath());
                cloudFile.setShowName(res.getName());
                cloudFile.setSize(String.valueOf(res.getSize()));
                cloudFile.setDir("dir".equals(res.getType()));
                cloudFile.setBytes((int) res.getSize());
                cloudFile.setRoot(res.getPath().getPrefix());
                items.add(cloudFile);
                if (cloudFile.isDir())
                    folders.add(cloudFile);
                else {
                    cloudFile.setFileType(res.getName().substring(res.getName().lastIndexOf(".") + 1));
                    files.add(cloudFile);
                }
            }
            cloudFolder.setContent(items);
            cloudFolder.setFiles(files);
            cloudFolder.setFolders(folders);
            return cloudFolder;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ServerIOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public FolderList getFolders(String path, Account account) throws TembooException, JSONException, UnsupportedEncodingException {
        return null;
    }

    @Override
    public String getDownloadFileLink(String path, Account account, boolean isFileContent) throws TembooException, JSONException {
        try {
            Resource resource = getFileMetadata(path, account);
            return resource.getPublicUrl();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public MultipartFile downloadFile(String path, Account account) throws TembooException, JSONException {
        File file = null;
        try {
            Resource resource = getFileMetadata(path, account);
            file = new File(resource.getName());
            restClient.downloadFile(path, file, new ProgressListener() {
                @Override
                public void updateProgress(long l, long l1) {
                }

                @Override
                public boolean hasCancelled() {
                    return false;
                }
            });
            return new MockMultipartFile(resource.getName(), resource.getName(), resource.getMimeType(), FileUtils.openInputStream(file));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ServerException e) {
            e.printStackTrace();
        } finally {
            if (file != null)
                file.delete();
        }
        return null;
    }


    @Override
    public boolean delete(String path, Account account) throws TembooException, JSONException {
        try {
            restClient.delete(path, true);
        } catch (ServerIOException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public String uploadFile(MultipartFile file, String path, Account account) throws TembooException, IOException, JSONException {
        try {
            Link link = restClient.getUploadLink(path + (!"disk:/".equals(path) ? "/" : "") + file.getOriginalFilename(), true);
            restClient.uploadFile(link, true, multipartToFile(file), new ProgressListener() {
                @Override
                public void updateProgress(long l, long l1) {

                }

                @Override
                public boolean hasCancelled() {
                    return false;
                }
            });
        } catch (ServerIOException e) {
            e.printStackTrace();
        } catch (WrongMethodException e) {
            e.printStackTrace();
        } catch (ServerException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public long getAvailableSize(Account account) throws TembooException, JSONException {
        return 0;
    }

    private String getQuery(List<NameValuePair> params) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        boolean first = true;

        for (NameValuePair pair : params) {
            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(pair.getName(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(pair.getValue(), "UTF-8"));
        }

        return result.toString();
    }

    public boolean createFolder(String path) throws TembooException, IOException {
        URL url = new URL("https://cloud-api.yandex.net/v1/disk/resources/?path=" + path);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        try {
            httpConn.setRequestMethod("PUT");
            String userpassword = CLIENT_ID + ":" + CLIENT_SECRET;
            String encodedAuthorization = Base64.getEncoder().encodeToString(userpassword.getBytes(StandardCharsets.UTF_8));
            httpConn.setRequestProperty("Authorization", "Basic " +
                    encodedAuthorization);

            int responseCode = httpConn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
            }
        } finally {
            httpConn.disconnect();
        }
        /*String folderName = null;
        try {
            folderName = URLEncoder.encode(path.replaceFirst("disk:", ""), "utf-8");
            restClient.makeFolder(folderName);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ServerIOException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }*/
        return true;
    }

    public Resource getFileMetadata(String path, Account account) throws TembooException, JSONException, UnsupportedEncodingException {
        YandexUser yandexUser = account.getYandexUser();
        try {
            return restClient.getResources(new ResourcesArgs.Builder().setPath(path).build());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ServerIOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public File multipartToFile(MultipartFile multipart) throws IllegalStateException, IOException {
        File convFile = new File(multipart.getOriginalFilename());
        multipart.transferTo(convFile);
        return convFile;
    }
}
