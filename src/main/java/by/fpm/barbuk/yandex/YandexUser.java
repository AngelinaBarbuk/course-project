package by.fpm.barbuk.yandex;

import by.fpm.barbuk.account.CloudUser;

import javax.crypto.SecretKey;
import java.util.Map;

/**
 * Created by B on 02.12.2016.
 */
public class YandexUser extends CloudUser {

    private String refreshToken;
    private Map<String, SecretKey> encryptionKeys;

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

    public Map<String, SecretKey> getEncryptionKeys() {
        return encryptionKeys;
    }

    public void setEncryptionKeys(Map<String, SecretKey> encryptionKeys) {
        this.encryptionKeys = encryptionKeys;
    }
}
