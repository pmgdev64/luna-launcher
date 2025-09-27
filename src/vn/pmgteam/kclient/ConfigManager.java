package vn.pmgteam.kclient;

import org.json.JSONArray;
import org.json.JSONObject;

import application.Main;

import java.io.*;
import java.util.ArrayList;

public class ConfigManager {

    private static final String CONFIG_FILE = Main.userHome + "/Appdata/Roaming/.luna/config.json";

    private JSONObject rootJson;
    private LauncherConfig config;

    public ConfigManager() {
        this.config = new LauncherConfig();
        load();
    }

    public LauncherConfig getConfig() {
        return config;
    }

    /** Load từ file JSON */
    public void load() {
        File file = new File(CONFIG_FILE);
        if (!file.exists()) {
            this.config = new LauncherConfig(); // default
            return;
        }
        try {
            String content = new String(java.nio.file.Files.readAllBytes(file.toPath()));
            rootJson = new JSONObject(content);

            // Load các giá trị của LauncherConfig
            config.gamePath = rootJson.optString("gamePath", Main.defaultGamePath);
            config.autoUpdate = rootJson.optBoolean("autoUpdate", true);
            config.theme = rootJson.optString("theme", "dark");
            config.lastVersion = rootJson.optString("lastVersion", "1.21"); // <-- fix

            // Load danh sách user
            config.users.clear();
            JSONArray arr = rootJson.optJSONArray("users");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject u = arr.getJSONObject(i);
                    UserProfile user = new UserProfile();
                    user.username = u.optString("username");
                    user.uuid = u.optString("uuid");
                    user.accessToken = u.optString("accessToken");
                    user.lastVersion = u.optString("lastVersion");
                    user.ramAllocated = u.optInt("ramAllocated", 2048);
                    config.users.add(user);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Save config vào file JSON */
    public void save() {
        try {
            JSONObject obj = new JSONObject();
            obj.put("gamePath", config.gamePath);
            obj.put("autoUpdate", config.autoUpdate);
            obj.put("theme", config.theme);
            obj.put("lastVersion", config.lastVersion); // <-- fix

            JSONArray arr = new JSONArray();
            for (UserProfile user : config.users) {
                JSONObject u = new JSONObject();
                u.put("username", user.username);
                u.put("uuid", user.uuid);
                u.put("accessToken", user.accessToken);
                u.put("lastVersion", user.lastVersion);
                u.put("ramAllocated", user.ramAllocated);
                arr.put(u);
            }
            obj.put("users", arr);

            File dir = new File(CONFIG_FILE).getParentFile();
            if (!dir.exists()) dir.mkdirs();
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                writer.write(obj.toString(4));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Thêm hoặc cập nhật user */
    public void addOrUpdateUser(UserProfile user) {
        config.users.removeIf(u -> u.username.equals(user.username));
        config.users.add(user);
        save();
    }

    /** Xóa user */
    public void removeUser(String username) {
        config.users.removeIf(u -> u.username.equals(username));
        save();
    }
}
