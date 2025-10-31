package vn.pmgteam.luna;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.json.*;

/**
 * Config system for launcher
 */
public class Config {

    private static final String CONFIG_FILE_NAME = "config.json";

    // Danh sách các phiên bản có sẵn
    public List<String> AvailableVersion = new ArrayList<>();

    // Username hiện tại
    public String userName = "";

    // JSON object chứa config
    private JSONObject jsonObjDataParse = new JSONObject();

    /**
     * Constructor
     * Nếu file config có tồn tại thì load, ngược lại tạo mặc định
     */
    public Config() {
        // Lấy user hiện tại
        this.userName = System.getProperty("user.name");

        // Load từ file nếu có
        if (Files.exists(Paths.get(CONFIG_FILE_NAME))) {
            try {
                String content = new String(Files.readAllBytes(Paths.get(CONFIG_FILE_NAME)), "UTF-8");
                loadConfigFromJson(content);
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Failed to read config file. Using default config.");
            }
        } else {
            // File config chưa có → tạo mặc định
            saveConfig(); // tạo file mặc định
        }
    }

    /**
     * Load config từ JSON string
     */
    public void loadConfigFromJson(String jsonText) {
        jsonObjDataParse = new JSONObject(jsonText);

        if (jsonObjDataParse.has("userName")) {
            this.userName = jsonObjDataParse.getString("userName");
        }

        if (jsonObjDataParse.has("AvailableVersion")) {
            AvailableVersion.clear();
            JSONArray arr = jsonObjDataParse.getJSONArray("AvailableVersion");
            for (int i = 0; i < arr.length(); i++) {
                AvailableVersion.add(arr.getString(i));
            }
        }
    }

    /**
     * Save config ra file JSON
     */
    public void saveConfig() {
        try {
            JSONObject obj = new JSONObject();
            obj.put("userName", this.userName);
            obj.put("AvailableVersion", this.AvailableVersion);

            try (FileWriter writer = new FileWriter(CONFIG_FILE_NAME)) {
                writer.write(obj.toString(4)); // indent 4 spaces
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to save config file.");
        }
    }

    /**
     * Thêm phiên bản mới vào danh sách và lưu config
     */
    public void addVersion(String version) {
        if (!AvailableVersion.contains(version)) {
            AvailableVersion.add(version);
            saveConfig();
        }
    }

    /**
     * Xóa phiên bản khỏi danh sách và lưu config
     */
    public void removeVersion(String version) {
        if (AvailableVersion.contains(version)) {
            AvailableVersion.remove(version);
            saveConfig();
        }
    }

    /**
     * Kiểm tra xem version đã có trong config chưa
     */
    public boolean hasVersion(String version) {
        return AvailableVersion.contains(version);
    }

    /**
     * Get JSON string của config
     */
    public String getConfigJsonString() {
        JSONObject obj = new JSONObject();
        obj.put("userName", this.userName);
        obj.put("AvailableVersion", this.AvailableVersion);
        return obj.toString(4);
    }

    /**
     * Cập nhật username và lưu config
     */
    public void setUserName(String userName) {
        this.userName = userName;
        saveConfig();
    }
}
