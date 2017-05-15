package by.fpm.barbuk.utils;

import com.temboo.core.TembooException;
import org.json.JSONException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;

public class DownloadHelper {

    public static MultipartFile downloadFile(String urlStr, String accessToken) throws TembooException, JSONException, IOException {
        CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
        URL url = new URL(urlStr);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        /*httpConn.setRequestMethod("GET");*/
        if (accessToken != null) {
            httpConn.setRequestProperty("Authorization", "Bearer " + accessToken);
        }
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
            return multipartFile;
        }
        return null;
    }

}
