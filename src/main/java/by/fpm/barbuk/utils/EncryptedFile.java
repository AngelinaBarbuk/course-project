package by.fpm.barbuk.utils;

import org.springframework.web.multipart.MultipartFile;

import javax.crypto.SecretKey;

/**
 * Created by B on 09.05.2017.
 */
public class EncryptedFile {
    private SecretKey secretKey;
    private MultipartFile file;

    public SecretKey getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(SecretKey secretKey) {
        this.secretKey = secretKey;
    }

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }
}
