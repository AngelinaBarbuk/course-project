package by.fpm.barbuk.uploadBigFile;

import by.fpm.barbuk.account.Account;
import by.fpm.barbuk.account.AccountService;
import com.temboo.core.TembooException;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
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

@Controller
public class BigFileController {
    @Autowired
    private AccountService accountService;
    @Autowired
    private BigFileHelper bigFileHelper;

    @ModelAttribute("module")
    String module() {
        return "bigFile";
    }

    @RequestMapping(value = "/bigFile/download", method = RequestMethod.GET)
    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    public void bigFileDownload(@RequestParam(name = "path") String path, HttpServletResponse response) throws JSONException, TembooException, IOException {
        MultipartFile file = bigFileHelper.downloadFile(path, getAccount());
        if (file != null) {
            response.setContentType(file.getContentType());
            response.setHeader("Content-Disposition", "attachment; filename=" + file.getOriginalFilename());
            IOUtils.copy(file.getInputStream(), response.getOutputStream());
            response.flushBuffer();
        }
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

    @RequestMapping(value = "/bigFile/delete", method = RequestMethod.DELETE)
    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    @ResponseBody
    public String delete(@RequestParam(name = "path") String path) throws JSONException, TembooException, IOException {
        Account account = getAccount();
        boolean result = bigFileHelper.delete(path, account);
        accountService.updateUsers(account);
        return "success";
    }

    private Account getAccount() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        return accountService.loadAccountByUsername(user.getUsername());
    }

    @RequestMapping("/bigFile")
    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    ModelAndView bigFile() throws Exception {
        if(bigFileHelper.collectClouds(getAccount()).isEmpty()){
            throw new Exception("No available clouds");
        }
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("bigFile/bigFile");
        Account account = getAccount();
        modelAndView.addObject("files", account.getUserBigFiles());
        return modelAndView;
    }
}
