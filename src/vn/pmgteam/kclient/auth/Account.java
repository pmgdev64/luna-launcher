package vn.pmgteam.kclient.auth;

public class Account {
    private String username;
    private String uuid;
    private String token;
    private boolean fullLicense;

    public Account(String username, String uuid, String token, boolean fullLicense) {
        this.username = username;
        this.uuid = uuid;
        this.token = token;
        this.fullLicense = fullLicense;
    }

    public String getUsername() { return username; }
    public String getUuid() { return uuid; }
    public String getToken() { return token; }
    public boolean isFullLicense() { return fullLicense; }
}
