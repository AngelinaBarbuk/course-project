package by.fpm.barbuk;

import com.temboo.Library.Google.OAuth.InitializeOAuth;
import com.temboo.core.TembooSession;

public class HelloTemboo {

    public static void main(String[] args) throws Exception {

        // Instantiate the Choreo, using a previously instantiated TembooSession object, eg:
        TembooSession session = new TembooSession("angelinabarbuk", "myFirstApp", "dz7TFlG60iXUQhhBkR4bBZ7jMQyK7U0A");
        InitializeOAuth initializeOAuthChoreo = new InitializeOAuth(session);
        // Get an InputSet object for the choreo
        InitializeOAuth.InitializeOAuthInputSet initializeOAuthInputs = initializeOAuthChoreo.newInputSet();
// Set credential to use for execution
        initializeOAuthInputs.setCredential("course");
// Set inputs
// Execute Choreo
        InitializeOAuth.InitializeOAuthResultSet initializeOAuthResults = initializeOAuthChoreo.execute(initializeOAuthInputs);
    }
}