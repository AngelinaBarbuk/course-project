package by.fpm.barbuk.dropbox;

/**
 * Created by B on 02.12.2016.
 */
public class DropboxUser implements java.io.Serializable {

    private String userId;
    private String accessToken;
    private String accessSecret;

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

    public String getAccessSecret() {
        return accessSecret;
    }

    public void setAccessSecret(String accessSecret) {
        this.accessSecret = accessSecret;
    }
}
