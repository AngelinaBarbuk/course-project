package by.fpm.barbuk.uploadBigFile;

import by.fpm.barbuk.account.Account;
import by.fpm.barbuk.account.AccountService;
import com.temboo.core.TembooException;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;

/**
 * Created by B on 02.12.2016.
 */
@Controller
public class BigFileController {
    @Autowired
    private AccountService accountService;

    private BigFileHelper bigFileHelper = new BigFileHelper();

    @ModelAttribute("module")
    String module() {
        return "bigFile";
    }

    @RequestMapping(value = "/bigFile/uploadFile", method = RequestMethod.POST)
    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    public String uploadBigFile(@RequestParam("file") MultipartFile file) throws JSONException, TembooException, IOException {
        Account account = getAccount();
        UserFiles userFiles = account.getUserBigFiles();
        bigFileHelper.uploadFile(file, account);
        account.setUserBigFiles(userFiles);
        accountService.updateUsers(account);
        return "redirect:/bigFile";
    }

    @RequestMapping(value = "/bigFile/download", method = RequestMethod.GET)
    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    public String dropboxDownload(@RequestParam(name = "path") String path) throws JSONException, TembooException, IOException {
        /*DropboxUser dropboxUser = getAccount().getDropboxUser();
        String result = dropboxHelper.getDownloadFileLink(path, dropboxUser);
        return "redirect:" + result;*/
        return "";
    }

    private Account getAccount() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        return accountService.loadAccountByUsername(user.getUsername());
    }

    @RequestMapping("/bigFile")
    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    ModelAndView bigFile() {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("bigFile/bigFile");
        Account account = getAccount();
        modelAndView.addObject("files", account.getUserBigFiles());
        return modelAndView;
    }
}