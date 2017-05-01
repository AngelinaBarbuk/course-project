package by.fpm.barbuk.google.drive;

import by.fpm.barbuk.account.Account;
import by.fpm.barbuk.account.AccountService;
import by.fpm.barbuk.cloudEntities.CloudFolder;
import by.fpm.barbuk.cloudEntities.FolderList;
import by.fpm.barbuk.support.web.AjaxRequestBody;
import by.fpm.barbuk.support.web.AjaxResponseBody;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.temboo.core.TembooException;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Created by B on 02.12.2016.
 */
@Controller
public class GoogleController {
    @Autowired
    private AccountService accountService;

    private GoogleHelper googleHelper = new GoogleHelper();
    private ObjectMapper mapper = new ObjectMapper();

    public static String getUrl(HttpServletRequest req) {
        String reqUrl = req.getRequestURL().toString();
        String queryString = req.getQueryString();   // d=789
        if (queryString != null) {
            reqUrl += "?" + queryString;
        }
        return reqUrl;
    }

    @ModelAttribute("module")
    String module() {
        return "google";
    }

    @RequestMapping(value = "/google/Ajax", method = RequestMethod.POST, produces = {"application/json; charset=UTF-8"})
    @ResponseStatus(HttpStatus.OK)
    public
    @ResponseBody
    String googleAjax(@RequestBody AjaxRequestBody requestBody) throws JSONException, TembooException, JsonProcessingException, UnsupportedEncodingException {
        if (requestBody.getRequestType().equals("Open_Dir")) {
            CloudFolder result = googleHelper.getFolderContent(requestBody.getPath(), getAccount());
            return mapper.writeValueAsString(result);
        }
        return mapper.writeValueAsString(new AjaxResponseBody());
    }

    @RequestMapping(value = "/google/folder", method = RequestMethod.GET)
    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    @ResponseStatus(HttpStatus.OK)
    public ModelAndView googlePath(@RequestParam(name = "path") String path) throws JSONException, TembooException, JsonProcessingException, UnsupportedEncodingException {
        CloudFolder result = googleHelper.getFolderContent(path, getAccount());
        result.setCloudStorage("/google");
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject("cloudFolder", result);
        modelAndView.setViewName("file-explorer/file-explorer");
        return modelAndView;
    }

    @RequestMapping(value = "/google/getFolders", method = RequestMethod.GET)
    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    @ResponseBody
    public FolderList dropboxGetFolders(@RequestParam(name = "path") String path) throws JSONException, TembooException, IOException {
        FolderList result = googleHelper.getFolders(path, getAccount());
        return result;
    }

    @RequestMapping(value = "/google/download", method = RequestMethod.GET)
    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    public String googleDownload(@RequestParam(name = "path", required = false) String path) throws JSONException, TembooException, IOException {
        String result = googleHelper.getDownloadFileLink(path, getAccount());
        /*MoveController.downloadFile(result, null);*/
        return "redirect:" + result;
    }

    @RequestMapping(value = "/google/delete", method = RequestMethod.DELETE)
    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    @ResponseBody
    public String googleDelete(@RequestParam(name = "path") String path) throws JSONException, TembooException, IOException {
        boolean result = googleHelper.delete(path, getAccount());
        return "success";
    }

    @RequestMapping(value = "/google/createFolder", method = RequestMethod.POST)
    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    @ResponseBody
    public String googleCreateFolder(@RequestParam(name = "path") String path, @RequestParam(name = "folderName") String folderName) throws JSONException, TembooException, IOException {
        boolean result = googleHelper.createFolder(path, folderName, getAccount());
        return "success";
    }

    @RequestMapping(value = "/google/uploadFile", method = RequestMethod.POST)
    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    public String googleUploadFile(@RequestParam(name = "path") String path, @RequestParam("file") MultipartFile file) throws JSONException, TembooException, IOException {
        googleHelper.uploadFile(file, path, getAccount());
        return "redirect:/google/folder?path=" + path;
    }

    private Account getAccount() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        return accountService.loadAccountByUsername(user.getUsername());
    }

    @RequestMapping("/google")
    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    String google() {
        Account account = getAccount();
        if (account.getGoogleUser() == null)
            return "redirect:/google/OAuth";
        return "redirect:/google/folder?path=root";
    }

    @RequestMapping("/google/OAuth")
    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    void googleOAuth(HttpServletResponse response) throws IOException {
        response.sendRedirect(googleHelper.getLoginUrl());
    }

    @RequestMapping("/google/OAuthLogIn")
    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    String googleOAuthLogin(HttpServletRequest request) throws IOException, TembooException, JSONException {
        Account account = getAccount();
        GoogleUser googleUser = googleHelper.getUserInfo(getUrl(request));
        if (googleUser != null) {
            account.setGoogleUser(googleUser);
            accountService.updateUsers(account);
        }
        return "forward:/google";
    }
}
