package vn.pmgteam.luna.util;

import org.json.JSONObject;

import vn.pmgteam.luna.util.UpdateInfo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.logging.log4j.*;

public class UpdateChecker {

    private static final String UPDATE_URL = "https://pmgdev64.github.io/api/update.json";
    
    public static Logger logIn = LogManager.getLogger("UpdateChecker");

    /**
     * Kiểm tra bản cập nhật mới nhất
     * @param premium true nếu là bản Premium
     * @return thông tin cập nhật hoặc null nếu có lỗi
     */
    public static UpdateInfo check(boolean premium) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(UPDATE_URL).openConnection();
            conn.setRequestProperty("User-Agent", "LunaLauncher/1.0");
            conn.connect();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null)
                    sb.append(line);

                JSONObject root = new JSONObject(sb.toString());
                JSONObject channel = root.getJSONObject(premium ? "premium" : "free");

                String version = channel.getString("version");
                String changelog = channel.optString("changelog", "(Không có ghi chú)");
                String downloadUrl = channel.getString("downloadUrl");

                return new UpdateInfo(version, changelog, downloadUrl, premium);
            }

        } catch (Exception e) {
            logIn.info("[UpdateChecker] Error: " + e.getMessage());
            return null;
        }
    }
}
