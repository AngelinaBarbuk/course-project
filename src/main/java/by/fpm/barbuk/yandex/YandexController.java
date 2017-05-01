package by.fpm.barbuk.yandex;

import by.fpm.barbuk.account.Account;
import by.fpm.barbuk.account.AccountService;
import by.fpm.barbuk.cloudEntities.CloudFolder;
import by.fpm.barbuk.cloudEntities.FolderList;
import by.fpm.barbuk.support.web.AjaxRequestBody;
import by.fpm.barbuk.support.web.AjaxResponseBody;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.temboo.core.TembooException;
import org.apache.commons.io.IOUtils;
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

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Created by B on 02.12.2016.
 */
@Controller
public class YandexController {
    @Autowired
    private AccountService accountService;

    private YandexHelper yandexHelper = new YandexHelper();
    private ObjectMapper mapper = new ObjectMapper();

    @ModelAttribute("module")
    String module() {
        return "yandex";
    }

    @RequestMapping(value = "/yandex/Ajax", method = RequestMethod.POST, produces = {"application/json; charset=UTF-8"})
    @ResponseStatus(HttpStatus.OK)
    public
    @ResponseBody
    String yandexAjax(@RequestBody AjaxRequestBody requestBody) throws JSONException, TembooException, JsonProcessingException, UnsupportedEncodingException {
        if (requestBody.getRequestType().equals("Open_Dir")) {
            CloudFolder result = yandexHelper.getFolderContent(requestBody.getPath(), getAccount());
            return mapper.writeValueAsString(result);
        }
        return mapper.writeValueAsString(new AjaxResponseBody());
    }

    @RequestMapping(value = "/yandex/folder", method = RequestMethod.GET)
    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    @ResponseStatus(HttpStatus.OK)
    public ModelAndView yandexPath(@RequestParam(name = "path") String path, HttpServletResponse response) throws JSONException, TembooException, JsonProcessingException, UnsupportedEncodingException {
        CloudFolder result = yandexHelper.getFolderContent(path/*new String(path.getBytes("Windows-1251"),"UTF-8")*/, getAccount());
        result.setCloudStorage("/yandex");
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject("cloudFolder", result);
        modelAndView.setViewName("file-explorer/file-explorer");
        return modelAndView;
    }

    @RequestMapping(value = "/yandex/download", method = RequestMethod.GET)
    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    public void yandexDownload(@RequestParam(name = "path") String path, HttpServletResponse response) throws JSONException, TembooException, IOException {
        MultipartFile file = yandexHelper.downloadFile(path, getAccount());
        if (file != null) {
            response.setContentType(file.getContentType());
            response.setHeader("Content-Disposition", "attachment; filename=" + file.getOriginalFilename());
            IOUtils.copy(file.getInputStream(), response.getOutputStream());
            response.flushBuffer();
        }
    }

    /*@RequestMapping(value = "/yandex/download", method = RequestMethod.GET)
    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    public String yandexDownload(@RequestParam(name = "path") String path) throws JSONException, TembooException, IOException {
        String result = yandexHelper.getDownloadFileLink(path, getAccount(), false);
        return "redirect:" + result;
    }*/

    @RequestMapping(value = "/yandex/getFolders", method = RequestMethod.GET)
    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    @ResponseBody
    public FolderList yandexGetFolders(@RequestParam(name = "path") String path) throws JSONException, TembooException, IOException {
        FolderList result = yandexHelper.getFolders(path, getAccount());
        return result;
    }

    @RequestMapping(value = "/yandex/delete", method = RequestMethod.DELETE)
    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    @ResponseBody
    public String yandexDelete(@RequestParam(name = "path") String path) throws JSONException, TembooException, IOException {
        boolean result = yandexHelper.delete(path, getAccount());
        return "success";
    }

    @RequestMapping(value = "/yandex/createFolder", method = RequestMethod.POST)
    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    @ResponseBody
    public String yandexCreateFolder(@RequestParam(name = "path") String path) throws JSONException, TembooException, IOException {
        yandexHelper.createFolder(path);
        /*boolean result = yandexHelper.createFolder(path, getAccount());*/
        return "success";
    }

    @RequestMapping(value = "/yandex/uploadFile", method = RequestMethod.POST)
    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    public void yandexUploadFile(@RequestParam(name = "path") String path, @RequestParam("file") MultipartFile file, HttpServletResponse response) throws JSONException, TembooException, IOException {
        yandexHelper.uploadFile(file, path, getAccount());
        response.setCharacterEncoding("UTF-8");
        response.sendRedirect("/yandex/folder?path=" + URLEncoder.encode(path, "utf-8"));
        /*return new String(("redirect:/yandex/folder?path=" + path).getBytes(), "Windows-1251");*/
    }

    private Account getAccount() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        return accountService.loadAccountByUsername(user.getUsername());
    }

    @RequestMapping("/yandex")
    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    String yandex() {
        Account account = getAccount();
        if (account.getYandexUser() == null)
            return "redirect:/yandex/OAuth";
        return "redirect:/yandex/folder?path=disk:/";
    }

    @RequestMapping("/yandex/OAuth")
    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    String yandexOAuth() {
        return "redirect:" + yandexHelper.getLoginUrl();
    }

    @RequestMapping("/yandex/OAuthLogIn")
    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    String yandexOAuthLogin(@RequestParam(name = "code") String code) throws IOException, TembooException, JSONException {
        Account account = getAccount();
        YandexUser yandexUser = yandexHelper.getUserInfo(code);
        if (yandexUser != null) {
            account.setYandexUser(yandexUser);
            accountService.updateUsers(account);
        }
        return "forward:/yandex";
    }
}
