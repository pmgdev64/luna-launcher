package vn.pmgteam.luna;

import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.json.*;

import application.Main;

public class LauncherConfig {
    public String gamePath = Main.defaultGamePath; // mặc định, hoặc Main.defaultGamePath
    public boolean autoUpdate = true;
    public String theme = "dark";
    public String lastVersion = "1.21"; // Phiên bản mặc định
    public List<UserProfile> users = new ArrayList<>();
    public String language = "vi"; // Mặc định là Vietnamese <--- TRƯỜNG MỚI ĐÃ ĐƯỢC THÊM

    // Command line cuối cùng (chứa skin offline)
    public static List<String> lastCommandLine = new ArrayList<>();

    private static final File CONFIG_FILE = new File(Main.lunaDir + "/config.json");

    public LauncherConfig() {
        load();
    }

    /** Lấy user theo username */
    public UserProfile getUser(String username) {
        for (UserProfile user : users) {
            if (user.username.equals(username)) return user;
        }
        return null;
    }

    /** Thêm hoặc cập nhật user */
    public void addOrUpdateUser(UserProfile user) {
        users.removeIf(u -> u.username.equals(user.username));
        users.add(user);
    }

    /** Xóa user theo username */
    public void removeUser(String username) {
        users.removeIf(u -> u.username.equals(username));
    }

    /** Lưu config ra file JSON */
    public void save() {
        try {
            JSONObject json = new JSONObject();
            json.put("language", language); // <--- LƯU TRƯỜNG LANGUAGE
            json.put("gamePath", gamePath);
            json.put("autoUpdate", autoUpdate);
            json.put("theme", theme);
            json.put("lastVersion", lastVersion);

            JSONArray userArr = new JSONArray();
            for (UserProfile u : users) userArr.put(u.toJSON());
            json.put("users", userArr);

            JSONArray cmdArr = new JSONArray();
            for (String s : lastCommandLine) cmdArr.put(s);
            json.put("lastCommandLine", cmdArr);

            CONFIG_FILE.getParentFile().mkdirs();
            Files.writeString(CONFIG_FILE.toPath(), json.toString(4), StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Load config từ file JSON */
    public void load() {
        try {
            if (!CONFIG_FILE.exists()) return;

            String content = Files.readString(CONFIG_FILE.toPath(), StandardCharsets.UTF_8);
            JSONObject json = new JSONObject(content);

            language = json.optString("language", language); // <--- TẢI TRƯỜNG LANGUAGE
            gamePath = json.optString("gamePath", gamePath);
            autoUpdate = json.optBoolean("autoUpdate", autoUpdate);
            theme = json.optString("theme", theme);
            lastVersion = json.optString("lastVersion", lastVersion);
            users.clear();
            JSONArray userArr = json.optJSONArray("users");
            if (userArr != null) {
                for (int i = 0; i < userArr.length(); i++) {
                    users.add(UserProfile.fromJSON(userArr.getJSONObject(i)));
                }
            }

            lastCommandLine.clear();
            JSONArray cmdArr = json.optJSONArray("lastCommandLine");
            if (cmdArr != null) {
                for (int i = 0; i < cmdArr.length(); i++) lastCommandLine.add(cmdArr.getString(i));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}