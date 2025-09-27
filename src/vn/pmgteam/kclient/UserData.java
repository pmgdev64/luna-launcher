package vn.pmgteam.kclient;

import org.json.JSONObject;
import java.io.*;
import java.nio.file.*;

public class UserData {
    private static final File CONFIG_DIR = new File(System.getProperty("user.home"), ".luna/config");
    private static final File USER_FILE = new File(CONFIG_DIR, "user.json");
    private static final File LAUNCHER_FILE = new File(CONFIG_DIR, "launcher.json");

    private JSONObject userConfig;
    private JSONObject launcherConfig;

    public UserData() {
        CONFIG_DIR.mkdirs();
        this.userConfig = load(USER_FILE);
        this.launcherConfig = load(LAUNCHER_FILE);
    }

    private JSONObject load(File file) {
        try {
            if (file.exists()) {
                String content = Files.readString(file.toPath());
                return new JSONObject(content);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new JSONObject();
    }

    private void save(File file, JSONObject obj) {
        try {
            Files.writeString(file.toPath(), obj.toString(2));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getUsername() {
        return userConfig.optString("username", "Player");
    }

    public void setUsername(String name) {
        userConfig.put("username", name);
        save(USER_FILE, userConfig);
    }

    public String getSelectedVersion() {
        return launcherConfig.optString("lastVersion", "latest-release");
    }

    public void setSelectedVersion(String ver) {
        launcherConfig.put("lastVersion", ver);
        save(LAUNCHER_FILE, launcherConfig);
    }

    public boolean isWindowed() {
        return launcherConfig.optBoolean("windowed", true);
    }

    public void setWindowed(boolean w) {
        launcherConfig.put("windowed", w);
        save(LAUNCHER_FILE, launcherConfig);
    }

    public int getWidth() {
        return launcherConfig.optInt("width", 854);
    }

    public int getHeight() {
        return launcherConfig.optInt("height", 480);
    }

    public void setResolution(int w, int h) {
        launcherConfig.put("width", w);
        launcherConfig.put("height", h);
        save(LAUNCHER_FILE, launcherConfig);
    }
}
