package vn.pmgteam.luna;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

/**
 * Module: AppUpdater.java
 * Chức năng: Cập nhật phiên bản Launcher thông qua API GitHub Releases.
 * Bản quyền: PmgTeam
 * Tác giả: PmgDev64
 */
public class AppUpdater {

    private static final Logger log = LogManager.getLogger("LunaLauncher");

    // API GitHub để lấy bản phát hành mới nhất
    private static final String UPDATE_URL = "https://api.github.com/repos/pmgdev64/luna-launcher/releases/latest";

    private static String remoteVersion = null;
    private static String remoteNotes = null;
    private static String remoteDownloadUrl = null;

    /**
     * Tiền tải thông tin bản phát hành mới nhất (không thông báo UI)
     */
    public static void fetchVersionInfoAsync() {
        CompletableFuture.runAsync(() -> {
            try {
                log.info("[Updater] Fetching GitHub release metadata...");
                String json = fetchUrl(UPDATE_URL);
                parseGitHubMetadata(json);
                log.info("[Updater] Metadata loaded: {}", remoteVersion);
            } catch (Exception e) {
                log.warn("[Updater] Failed to fetch metadata: {}", e.toString());
            }
        });
    }

    /**
     * Kiểm tra cập nhật khi người dùng thao tác (nhấn nút)
     */
    public static void checkForUpdate(String currentVersion) {
        try {
            log.info("[Updater] Checking for updates...");
            if (remoteVersion == null) {
                String json = fetchUrl(UPDATE_URL);
                parseGitHubMetadata(json);
            }

            if (isNewer(currentVersion, remoteVersion)) {
                log.info("[Updater] New version detected: {}", remoteVersion);
                log.info("[Updater] Download URL: {}", remoteDownloadUrl);
                // TODO: Hiển thị UI xác nhận cập nhật (ví dụ UpdateDialog)
                // UpdateDialog.show(remoteVersion, remoteNotes, remoteDownloadUrl);
            } else {
                log.info("[Updater] You are on the latest version: {}", currentVersion);
            }

        } catch (Exception e) {
            log.error("[Updater] Lỗi khi cập nhật: {}", e.toString(), e);
        }
    }

    /**
     * So sánh phiên bản, hỗ trợ dạng: v0.0.04, beta-test-4, dev-preview-7, v1.0.0
     */
    private static boolean isNewer(String local, String remote) {
        if (local == null || remote == null) return false;

        // Nếu giống hệt chuỗi
        if (local.equalsIgnoreCase(remote)) return false;

        try {
            // Lấy phần số cuối cùng trong chuỗi (nếu có)
            String localSuffix = local.replaceAll(".*?(\\d+)$", "$1");
            String remoteSuffix = remote.replaceAll(".*?(\\d+)$", "$1");

            int localNum = Integer.parseInt(localSuffix);
            int remoteNum = Integer.parseInt(remoteSuffix);

            // Nếu tên gốc khác nhau (ví dụ beta-test vs v), coi là khác branch
            boolean branchDifferent = !local.replaceAll("\\d", "")
                    .equalsIgnoreCase(remote.replaceAll("\\d", ""));

            return (remoteNum > localNum) || branchDifferent;

        } catch (Exception e) {
            // Nếu không parse được thì so sánh chuỗi theo alphabet
            return remote.compareToIgnoreCase(local) > 0;
        }
    }


    /**
     * Phân tích JSON từ GitHub API
     */
    private static void parseGitHubMetadata(String json) {
        if (json == null || json.isEmpty()) return;
        try {
            remoteVersion = extract(json, "\"tag_name\"\\s*:\\s*\"(.*?)\"");
            remoteNotes = extract(json, "\"body\"\\s*:\\s*\"(.*?)\"");
            remoteDownloadUrl = extract(json, "\"browser_download_url\"\\s*:\\s*\"(.*?)\"");
        } catch (Exception e) {
            log.error("[Updater] Error parsing metadata: {}", e.toString(), e);
        }
    }

    /**
     * Fetch nội dung từ URL
     */
    private static String fetchUrl(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(4000);
        conn.setReadTimeout(4000);
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "LunaLauncher-Updater");

        if (conn.getResponseCode() == 404) {
            log.warn("[Updater] GitHub API returned 404 (not found)");
            return "{}";
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    private static String extract(String text, String regex) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(regex).matcher(text);
        return m.find() ? m.group(1).replace("\\n", "\n") : null;
    }
}
