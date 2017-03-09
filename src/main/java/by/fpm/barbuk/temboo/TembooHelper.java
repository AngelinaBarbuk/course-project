package by.fpm.barbuk.temboo;

import com.temboo.core.TembooSession;

import java.security.SecureRandom;

/**
 * Created by B on 01.03.2017.
 */
public abstract class TembooHelper {

    // Replace with your Temboo credentials.
    protected static final String TEMBOO_ACCOUNT_NAME = "angelinabarbukyandex";
    protected static final String TEMBOO_APP_KEY_NAME = "myFirstApp";
    protected static final String TEMBOO_APP_KEY_VALUE = "uNiSBe4QkSlOtZ1a7Zozh4sERQfQOdQh";

    protected TembooSession session = null;

    protected String tokenSecret = "";
    protected String callbackID = "";

    protected String stateToken;

    public TembooHelper() {
        generateStateToken();
        try {
            session = new TembooSession(TEMBOO_ACCOUNT_NAME, TEMBOO_APP_KEY_NAME, TEMBOO_APP_KEY_VALUE);
        } catch (Exception te) {
            te.printStackTrace();
        }
    }

    private void generateStateToken() {
        SecureRandom random = new SecureRandom();
        stateToken = "dropbox-" + random.nextInt();
    }


}
