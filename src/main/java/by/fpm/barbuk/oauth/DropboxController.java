package by.fpm.barbuk.oauth;

import by.fpm.barbuk.account.Account;
import by.fpm.barbuk.account.AccountService;
import by.fpm.barbuk.dropbox.CloudFolder;
import by.fpm.barbuk.dropbox.DropboxUser;
import by.fpm.barbuk.support.web.AjaxRequestBody;
import by.fpm.barbuk.support.web.AjaxResponseBody;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.temboo.core.TembooException;
import org.apache.http.HttpResponse;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by B on 02.12.2016.
 */
@Controller
public class DropboxController {
    @Autowired
    private AccountService accountService;

    private DropboxAuthHelper dropboxAuthHelper = new DropboxAuthHelper();
    private ObjectMapper mapper = new ObjectMapper();

    @ModelAttribute("module")
    String module() {
        return "dropbox";
    }

    @RequestMapping(value = "/dropbox/Ajax", method = RequestMethod.POST, produces = {"application/json; charset=UTF-8"})
    @ResponseStatus(HttpStatus.OK)
    public
    @ResponseBody
    String dropboxAjax(@RequestBody AjaxRequestBody requestBody) throws JSONException, TembooException, JsonProcessingException, UnsupportedEncodingException {
        if (requestBody.getRequestType().equals("Open_Dir")) {
            DropboxUser dropboxUser = getAccount().getDropboxUser();
            CloudFolder result = dropboxAuthHelper.getFolderContent(requestBody.getPath(), dropboxUser);
            return mapper.writeValueAsString(result);
        }
        return mapper.writeValueAsString(new AjaxResponseBody());
    }

    @RequestMapping(value = "/dropbox/folder")
    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    @ResponseStatus(HttpStatus.OK)
    public ModelAndView dropboxPath(@RequestParam(name="path") String path, HttpServletResponse response) throws JSONException, TembooException, JsonProcessingException, UnsupportedEncodingException {
        DropboxUser dropboxUser = getAccount().getDropboxUser();
        CloudFolder result = dropboxAuthHelper.getFolderContent(path, dropboxUser);
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject("cloudFolder", result);
        response.setCharacterEncoding("UTF-8");
        modelAndView.setViewName("file-explorer/file-explorer");
        return modelAndView;
    }

    @RequestMapping(value = "/dropbox/download")
    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    public String dropboxDownload(@RequestParam(name="path") String path, HttpServletResponse response) throws JSONException, TembooException, IOException {
        DropboxUser dropboxUser = getAccount().getDropboxUser();
        String result = dropboxAuthHelper.getDownloadFileLink(path, dropboxUser);
        return "redirect:" + result;
    }

    @RequestMapping(value = "/dropbox/delete")
    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    public String dropboxDelete(@RequestParam(name="path") String path, HttpServletResponse response) throws JSONException, TembooException, IOException {
        DropboxUser dropboxUser = getAccount().getDropboxUser();
        String result = dropboxAuthHelper.getDownloadFileLink(path, dropboxUser);
        return "redirect:" + result;
    }

    private Account getAccount() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        return accountService.loadAccountByUsername(user.getUsername());
    }

    @RequestMapping("/dropbox")
    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    String dropbox() {
        Account account = getAccount();
        if (account.getDropboxUser() == null)
            return "forward:/dropbox/OAuth";
        return "forward:/dropbox/folder?path=root";
    }

    @RequestMapping("/dropbox/OAuth")
    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    String dropboxOAuth() {
        return "redirect:" + dropboxAuthHelper.getLoginUrl();
    }

    @RequestMapping("/dropbox/OAuthLogIn")
    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    String dropboxOAuthLogin() throws IOException, TembooException, JSONException {
        Account account = getAccount();
        DropboxUser dropboxUser = dropboxAuthHelper.getUserInfo();
        if (dropboxUser != null) {
            account.setDropboxUser(dropboxUser);
            accountService.updateDropboxUser(account);
        }
        return "forward:/dropbox";
    }
}
