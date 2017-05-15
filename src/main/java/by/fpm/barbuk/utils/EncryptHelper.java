package by.fpm.barbuk.utils;

import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.*;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class EncryptHelper {

    private KeyGenerator keyGen = KeyGenerator.getInstance("AES");
    private SecureRandom random = new SecureRandom();
    private Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");

    public EncryptHelper() throws NoSuchAlgorithmException, NoSuchPaddingException {
        keyGen.init(random);
    }

    public EncryptedFile encrypt(MultipartFile file) {
        try {
            SecretKey secretKey = keyGen.generateKey();
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encrypted = cipher.doFinal(file.getBytes());
            MultipartFile encryptedMultipartFile = new MockMultipartFile(file.getName(), file.getOriginalFilename(), file.getContentType(), encrypted);
            EncryptedFile encryptedFile = new EncryptedFile();
            encryptedFile.setFile(encryptedMultipartFile);
            encryptedFile.setSecretKey(secretKey);
            return encryptedFile;
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public MultipartFile decrypt(SecretKey secretKey, MultipartFile file) {
        try {
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decrypted = cipher.doFinal(file.getBytes());
            MultipartFile multipartFile = new MockMultipartFile(file.getName(), file.getOriginalFilename(), file.getContentType(), decrypted);
            return multipartFile;
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
