package by.fpm.barbuk.uploadBigFile;

import org.springframework.web.multipart.MultipartFile;

import javax.crypto.SecretKey;
import java.io.Serializable;

/**
 * Created by B on 01.03.2017.
 */
public class FilePart implements Serializable {
    private String cloudName;
    private String path;
    private SecretKey secretKey;
    private MultipartFile file;

    public String getCloudName() {
        return cloudName;
    }

    public void setCloudName(String cloudName) {
        this.cloudName = cloudName;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

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
