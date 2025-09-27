package vn.pmgteam.kclient;

import java.util.ArrayList;
import java.util.List;

import application.Main;

public class UserProfile {
    public String username;
    public String uuid;
    public String accessToken;
    public String lastVersion;
    public int ramAllocated;

    public UserProfile() {} // default constructor

    public UserProfile(String username, String uuid, String accessToken, String lastVersion, int ram) {
        this.username = username;
        this.uuid = uuid;
        this.accessToken = accessToken;
        this.lastVersion = lastVersion;
        this.ramAllocated = ram;
    }
}
