package by.fpm.barbuk.google.drive;

import by.fpm.barbuk.account.CloudUser;

/**
 * Created by B on 02.12.2016.
 */
public class GoogleUser extends CloudUser {

    private String refreshToken;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
