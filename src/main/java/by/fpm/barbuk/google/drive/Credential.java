package by.fpm.barbuk.google.drive;

import com.google.api.client.auth.oauth2.BearerToken;

import java.io.Serializable;

/**
 * Created by B on 01.05.2017.
 */
public class Credential extends com.google.api.client.auth.oauth2.Credential implements Serializable {
    public Credential() {
        super(BearerToken.authorizationHeaderAccessMethod());
    }

    public Credential(AccessMethod method) {
        super(method);
    }

    protected Credential(Builder builder) {
        super(builder);
    }

    public static class Builder extends com.google.api.client.auth.oauth2.Credential.Builder {

        public Builder(AccessMethod method) {
            super(method);
        }

        public Credential build() {
            return new Credential(this);
        }
    }
}
