package vn.pmgteam.luna;

import org.json.JSONObject;

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

    /** Chuyển UserProfile thành JSONObject */
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("username", username);
        json.put("uuid", uuid);
        json.put("accessToken", accessToken);
        json.put("lastVersion", lastVersion);
        json.put("ramAllocated", ramAllocated);
        return json;
    }

    /** Tạo UserProfile từ JSONObject */
    public static UserProfile fromJSON(JSONObject json) {
        UserProfile user = new UserProfile();
        user.username = json.optString("username", "");
        user.uuid = json.optString("uuid", "");
        user.accessToken = json.optString("accessToken", "");
        user.lastVersion = json.optString("lastVersion", "1.21");
        user.ramAllocated = json.optInt("ramAllocated", 1024);
        return user;
    }
}
