package by.fpm.barbuk;

import by.fpm.barbuk.account.Account;
import by.fpm.barbuk.account.AccountService;
import by.fpm.barbuk.dropbox.DropboxHelper;
import by.fpm.barbuk.google.drive.GoogleHelper;
import by.fpm.barbuk.temboo.CloudHelper;
import com.temboo.core.TembooException;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;

@Controller
public class MoveController {
    @Autowired
    private AccountService accountService;

    private GoogleHelper googleHelper = new GoogleHelper();
    private CloudHelper dropboxHelper = new DropboxHelper();

    @RequestMapping(value = "/dropbox_to_google", method = RequestMethod.POST)
    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    @ResponseBody
    public String dropboxToGoogle(@RequestParam(name = "fileToMove") String path, @RequestParam(name = "pathToMove") String folderName) throws JSONException, TembooException, IOException {
        Account account = getAccount();
        String url = dropboxHelper.getDownloadFileLink(path, account, false);
        MultipartFile file = downloadFile(url, null);
        googleHelper.uploadFile(file, folderName, account);
        return "success";
    }

    @RequestMapping(value = "/google_to_dropbox", method = RequestMethod.POST)
    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    @ResponseBody
    public String googleToDropbox(@RequestParam(name = "fileToMove") String path, @RequestParam(name = "pathToMove") String folderName) throws JSONException, TembooException, IOException {
        Account account = getAccount();
        String url = googleHelper.getDownloadFileLink(path, account, false);
        MultipartFile file = downloadFile(url, null);
        dropboxHelper.uploadFile(file, folderName, account);
        return "success";
    }

    private Account getAccount() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        return accountService.loadAccountByUsername(user.getUsername());
    }

    public static MultipartFile downloadFile(String urlStr, String accessToken) throws TembooException, JSONException, IOException {
        CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
        URL url = new URL(urlStr);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        if(accessToken!=null) {
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
