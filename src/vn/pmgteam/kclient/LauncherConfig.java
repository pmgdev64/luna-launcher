package vn.pmgteam.kclient;

import java.util.ArrayList;
import java.util.List;

public class LauncherConfig {
    public String gamePath = "C:/Minecraft"; // mặc định, hoặc Main.defaultGamePath
    public boolean autoUpdate = true;
    public String theme = "dark";
    public String lastVersion = "1.21"; // Phiên bản mặc định
    public List<UserProfile> users = new ArrayList<>();

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
}
