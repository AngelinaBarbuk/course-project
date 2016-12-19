package by.fpm.barbuk.account;

import by.fpm.barbuk.dropbox.DropboxUser;
import by.fpm.barbuk.google.drive.GoogleUser;
import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.security.PrivateKey;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("serial")
@Entity
@Table(name = "account")
public class Account implements java.io.Serializable {

    @Id
    @GeneratedValue
    private Long id;

    @Column(unique = true)
    private String email;

    @JsonIgnore
    private String password;

    private String role = "ROLE_USER";

    private Instant created;

    @Column(name = "dropbox_user", columnDefinition = "LONGVARBINARY")
    private DropboxUser dropboxUser;

    @Column(name = "google_user", columnDefinition = "LONGVARBINARY")
    private GoogleUser googleUser;


    protected Account() {

    }

    public Account(String email, String password, String role) {
        this.email = email;
        this.password = password;
        this.role = role;
        this.created = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Instant getCreated() {
        return created;
    }

    public DropboxUser getDropboxUser() {
        return dropboxUser;
    }

    public void setDropboxUser(DropboxUser dropboxUser) {
        this.dropboxUser = dropboxUser;
    }

    public GoogleUser getGoogleUser() {
        return googleUser;
    }

    public void setGoogleUser(GoogleUser googleUser) {
        this.googleUser = googleUser;
    }
}
