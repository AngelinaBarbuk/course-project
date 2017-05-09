package by.fpm.barbuk.account;


import by.fpm.barbuk.dropbox.DropboxHelper;
import by.fpm.barbuk.google.drive.GoogleHelper;
import by.fpm.barbuk.yandex.YandexHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class AuthenticationSuccessHandlerImpl extends SimpleUrlAuthenticationSuccessHandler {
    @Autowired
    private DropboxHelper dropboxHelper;
    @Autowired
    private GoogleHelper googleHelper;
    @Autowired
    private YandexHelper yandexHelper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        if(dropboxHelper!=null) {
            dropboxHelper.setClient(null);
        }
        if(googleHelper!=null){
            googleHelper.setCredential(null);
        }
        if(yandexHelper!=null){
            yandexHelper.setCredentials(null);
        }
        response.sendRedirect("/");
    }
}
