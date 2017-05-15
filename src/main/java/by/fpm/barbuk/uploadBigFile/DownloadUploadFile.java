package by.fpm.barbuk.uploadBigFile;

import by.fpm.barbuk.account.Account;
import org.springframework.web.multipart.MultipartFile;

/**
 * Created by B on 08.05.2017.
 */
public interface DownloadUploadFile {

    int CHUNKED_UPLOAD_MAX_ATTEMPTS = 3;

    MultipartFile downloadFile(String path, Account account);

    String uploadFile(MultipartFile file, String path, Account account);

    boolean delete(String path, Account account);
}
