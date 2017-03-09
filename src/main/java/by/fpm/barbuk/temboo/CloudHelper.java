package by.fpm.barbuk.temboo;

import by.fpm.barbuk.account.Account;
import by.fpm.barbuk.account.CloudUser;
import by.fpm.barbuk.cloudEntities.CloudFolder;
import by.fpm.barbuk.cloudEntities.FolderList;
import com.temboo.core.TembooException;
import org.json.JSONException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Created by B on 09.03.2017.
 */
public interface CloudHelper {
    String getLoginUrl();

    String getStateToken();

    CloudUser getUserInfo() throws IOException;

    CloudFolder getFolderContent(String path, Account account) throws TembooException, JSONException, UnsupportedEncodingException;

    FolderList getFolders(String path, Account account) throws TembooException, JSONException, UnsupportedEncodingException;

    String getDownloadFileLink(String path, Account account) throws TembooException, JSONException;

    boolean delete(String path, Account account) throws TembooException, JSONException;

    /*boolean createFolder(String path, Account account) throws TembooException;*/

    boolean uploadFile(MultipartFile file, String path, Account account) throws TembooException, IOException, JSONException;

    /*Account encrypt(String path, Account account) throws TembooException, IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, JSONException;*/
}
