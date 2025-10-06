package vn.pmgteam.kclient;

import org.json.*;
import application.Main;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class MinecraftLauncher {
    private static final String MANIFEST_URL = "https://launchermeta.mojang.com/mc/game/version_manifest.json";
    public static final File DOT_MINECRAFT = new File(Main.defaultGamePath);

    // === SESSION RECORD ===
    public record Session(String username, String accessToken, String uuid) {}

    // === CALLBACK ===
    public interface ProgressCallback {
        void onProgress(double progress, String status);
    }
    
    public static LauncherConfig launcherConfig = new LauncherConfig();

    // === FETCH AVAILABLE VERSIONS ===
    public static List<String> fetchAvailableVersions() {
        List<String> versions = new ArrayList<>();
        try {
            URL url = new URL(MANIFEST_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "MinecraftLauncher/1.0");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();

            JSONObject manifest = new JSONObject(sb.toString());
            JSONArray arr = manifest.getJSONArray("versions");
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                versions.add(obj.getString("id"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return versions;
    }


    // === CHECK INSTALLED VERSION ===
    public static boolean isVersionInstalled(String version) {
        File versionDir = new File(DOT_MINECRAFT, "versions/" + version);
        File versionJson = new File(versionDir, version + ".json");
        File versionJar  = new File(versionDir, version + ".jar");
        return versionDir.exists() && versionJson.exists() && versionJar.exists();
    }

    // === CHECK LEGACY VERSION ===
    public static boolean isLegacyVersion(String version) {
        String v = version.toLowerCase(Locale.ROOT);
        return v.startsWith("a") || v.startsWith("inf") || v.startsWith("c") || v.startsWith("rd") || v.startsWith("b");
    }
    
 // Tải và parse version.json từ thư mục versions/<id>/<id>.json
    private static JSONObject loadVersionJson(String versionId) throws IOException {
        File versionJsonFile = new File(DOT_MINECRAFT, "versions/" + versionId + "/" + versionId + ".json");
        if (!versionJsonFile.exists()) {
            throw new FileNotFoundException("Version JSON not found: " + versionJsonFile.getAbsolutePath());
        }
        String json = new String(Files.readAllBytes(versionJsonFile.toPath()), StandardCharsets.UTF_8);
        return new JSONObject(json);
    }

    // Xác định đường dẫn file library từ thông tin JSON
    private static File getLibraryFile(JSONObject lib) {
        String name = lib.getString("name"); 
        // ví dụ: "org.lwjgl.lwjgl:lwjgl:2.9.1"
        String[] parts = name.split(":");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid library name: " + name);
        }

        String group = parts[0].replace('.', '/');
        String artifact = parts[1];
        String version = parts[2];

        File libFile = new File(DOT_MINECRAFT, "libraries/" 
                + group + "/" 
                + artifact + "/" 
                + version + "/" 
                + artifact + "-" + version + ".jar");

        return libFile;
    }


    // === MAIN LAUNCH ===
    /*public static void launch(String version, Session session, ProgressCallback callback) throws Exception {
        File versionDir = new File(DOT_MINECRAFT, "versions/" + version);
        File versionJson = new File(versionDir, version + ".json");
        File versionJar  = new File(versionDir, version + ".jar");

        // Nếu là legacy (rd, c, inf, a, b) thì chỉ cần jar
        if (isLegacyVersion(version)) {
            if (!versionJar.exists()) {
                throw new IOException("Legacy jar not found: " + versionJar.getAbsolutePath());
            }

            callback.onProgress(1.0, "Launching legacy game...");

            List<String> cmd = new ArrayList<>();
            cmd.add(new File(System.getProperty("java.home"), "bin/java.exe").getAbsolutePath());
            cmd.add("-Xmx512M");
            cmd.add("-cp");
            cmd.add(versionJar.getAbsolutePath());

            // Legacy main class mapping
            String mainClass;
            if (version.startsWith("rd")) {
                mainClass = "com.mojang.rubydung.RubyDung";
            } else {
                mainClass = "com.mojang.minecraft.Minecraft";
            }

            cmd.add(mainClass);
            cmd.add(session.username());

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(DOT_MINECRAFT);
            pb.inheritIO();
            pb.start();
            return;
        }

        // === Modern (1.6+) ===
        if (!versionJson.exists()) {
            callback.onProgress(0, "Downloading version metadata...");
            downloadVersionJson(version);
        }

        if (!versionJar.exists()) {
            callback.onProgress(0.1, "Downloading client jar...");
            downloadClientJar(version);
        }

        callback.onProgress(0.3, "Downloading libraries...");
        downloadLibraries(version, callback);

        callback.onProgress(0.7, "Downloading assets...");
        downloadAssets(version, callback);

        // Load version JSON
        JSONObject versionData = new JSONObject(Files.readString(versionJson.toPath()));
        String mainClass = versionData.optString("mainClass", "net.minecraft.client.main.Main");

        List<String> cmd = new ArrayList<>();
        cmd.add(new File(System.getProperty("java.home"), "bin/java.exe").getAbsolutePath());
        cmd.add("-Xmx2G");
        cmd.add("-Xms1G");

        // natives path
        cmd.add("-Djava.library.path=" + new File(versionDir, "natives").getAbsolutePath());
        cmd.add("-cp");
        cmd.add(getClasspath(version));

        // Main class
        cmd.add(mainClass);

        // === Authentication & game args ===
        cmd.add("--username");   cmd.add(session.username());
        cmd.add("--version");    cmd.add(version);
        cmd.add("--gameDir");    cmd.add(DOT_MINECRAFT.getAbsolutePath());
        cmd.add("--assetsDir");  cmd.add(new File(DOT_MINECRAFT, "assets").getAbsolutePath());
        cmd.add("--accessToken");cmd.add(session.accessToken() != null ? session.accessToken() : "0");
        cmd.add("--uuid");       cmd.add(session.uuid() != null ? session.uuid() : "0");

        callback.onProgress(1.0, "Launching game...");

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(DOT_MINECRAFT);
        pb.inheritIO();
        pb.start();
    }*/
    public static void launch(String version, Session session, ProgressCallback callback) throws Exception {
        File versionDir = new File(DOT_MINECRAFT, "versions/" + version);
        File versionJson = new File(versionDir, version + ".json");
        File versionJar  = new File(versionDir, version + ".jar");

        // Chọn Java path theo version
        String javaPath = selectJavaPath(version);

        // === LEGACY VERSION ===
        if (isLegacyVersion(version)) {
            if (!versionJar.exists()) {
                throw new IOException("Legacy jar not found: " + versionJar.getAbsolutePath());
            }

            callback.onProgress(1.0, I18n.get("legacy.loading"));

            List<String> cmd = new ArrayList<>();
            cmd.add(javaPath);
            cmd.add("-Xmx512M");
            cmd.add("-cp");
            cmd.add(versionJar.getAbsolutePath());

            String mainClass = version.startsWith("rd") ? "com.mojang.rubydung.RubyDung" : "com.mojang.minecraft.Minecraft";
            cmd.add(mainClass);
            cmd.add(session.username());

            // **Lưu commandLine**
            LauncherConfig.lastCommandLine = new ArrayList<>(cmd);
            launcherConfig.save();

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(DOT_MINECRAFT);
            pb.inheritIO();
            pb.start();
            return;
        }

        // === MODERN (1.6+) ===
        if (!versionJson.exists()) {
            callback.onProgress(0, I18n.get("version.metadata"));
            downloadVersionJson(version);
        }

        if (!versionJar.exists()) {
            callback.onProgress(0.1, I18n.get("client.jarfile"));
            downloadClientJar(version);
        }

        callback.onProgress(0.3, I18n.get("libraries.download"));
        downloadLibraries(version, callback);

        callback.onProgress(0.7, I18n.get("assets.download"));
        downloadAssets(version, callback);

        callback.onProgress(0.9, I18n.get("natives.extract"));
        extractNatives(version);

        // Load version JSON
        JSONObject versionData = new JSONObject(Files.readString(versionJson.toPath()));
        String mainClass = versionData.optString("mainClass", "net.minecraft.client.main.Main");

        List<String> cmd = new ArrayList<>();
        cmd.add(javaPath); // Java path
        cmd.add("-Xmx2G");
        cmd.add("-Xms1G");

        File nativesDir = new File(versionDir, "natives");
        cmd.add("-Djava.library.path=" + nativesDir.getAbsolutePath());
        cmd.add("-Dorg.lwjgl.librarypath=" + nativesDir.getAbsolutePath());

        // Classpath
        cmd.add("-cp");
        cmd.add(getClasspath(version));

        // Main class
        cmd.add(mainClass);

        // Game args
        cmd.add("--username");      cmd.add(session.username());
        cmd.add("--version");       cmd.add(version);
        cmd.add("--gameDir");       cmd.add(DOT_MINECRAFT.getAbsolutePath());
        cmd.add("--assetsDir");     cmd.add(new File(DOT_MINECRAFT, "assets").getAbsolutePath());
        cmd.add("--accessToken");   cmd.add(session.accessToken() != null ? session.accessToken() : "0");
        cmd.add("--uuid");          cmd.add(session.uuid() != null ? session.uuid() : "0");
        cmd.add("--userProperties");cmd.add("{}");

        // === Lưu commandLine vào config ===
        launcherConfig.lastCommandLine = new ArrayList<>(cmd);
        launcherConfig.save();

        callback.onProgress(1.0, I18n.get("game.launching"));

        // Start game
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(DOT_MINECRAFT);
        pb.inheritIO();
        pb.start();

        System.out.println("Minecraft Arguments: " + cmd);
    }

    // === CHỌN JAVA THEO VERSION ===
    private static String selectJavaPath(String version) {
    	return "C:\\Program Files\\Java\\jre1.8.0_202\\bin\\java.exe";
    }



    // === DOWNLOAD VERSION JSON ===
    private static void downloadVersionJson(String version) throws Exception {
        JSONObject manifest = new JSONObject(readURL(MANIFEST_URL));
        JSONArray versions = manifest.getJSONArray("versions");
        String url = null;
        for (int i = 0; i < versions.length(); i++) {
            JSONObject obj = versions.getJSONObject(i);
            if (obj.getString("id").equals(version)) {
                url = obj.getString("url");
                break;
            }
        }
        if (url == null) throw new IOException("Version not found: " + version);

        String json = readURL(url);
        File versionDir = new File(DOT_MINECRAFT, "versions/" + version);
        versionDir.mkdirs();
        Files.writeString(new File(versionDir, version + ".json").toPath(), json);
    }

    // === DOWNLOAD CLIENT JAR ===
    private static void downloadClientJar(String version) throws Exception {
        File versionJson = new File(DOT_MINECRAFT, "versions/" + version + "/" + version + ".json");
        JSONObject json = new JSONObject(Files.readString(versionJson.toPath()));
        JSONObject downloads = json.getJSONObject("downloads").getJSONObject("client");
        String url = downloads.getString("url");

        File jarFile = new File(DOT_MINECRAFT, "versions/" + version + "/" + version + ".jar");
        downloadFile(url, jarFile);
    }

    // === DOWNLOAD LIBRARIES ===
    private static void downloadLibraries(String version, ProgressCallback callback) throws Exception {
        File versionJson = new File(DOT_MINECRAFT, "versions/" + version + "/" + version + ".json");
        JSONObject json = new JSONObject(Files.readString(versionJson.toPath()));
        JSONArray libs = json.getJSONArray("libraries");

        int total = libs.length();
        for (int i = 0; i < total; i++) {
            JSONObject lib = libs.getJSONObject(i);
            if (!lib.has("downloads")) continue;
            JSONObject artifact = lib.getJSONObject("downloads").optJSONObject("artifact");
            if (artifact == null) continue;

            String url = artifact.getString("url");
            String path = artifact.getString("path");
            File file = new File(DOT_MINECRAFT, "libraries/" + path);
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                downloadFile(url, file);
            }
            double progress = 0.3 + (0.4 * ((i + 1) / (double) total));
            callback.onProgress(progress, I18n.get("library.path") + path);
        }
    }

    // === DOWNLOAD ASSETS ===
    private static void downloadAssets(String version, ProgressCallback callback) throws Exception {
        File versionJson = new File(DOT_MINECRAFT, "versions/" + version + "/" + version + ".json");
        JSONObject json = new JSONObject(Files.readString(versionJson.toPath()));
        JSONObject assetIndex = json.getJSONObject("assetIndex");

        String url = assetIndex.getString("url");
        JSONObject assets = new JSONObject(readURL(url)).getJSONObject("objects");

        int total = assets.length();
        int index = 0;
        for (String key : assets.keySet()) {
            JSONObject obj = assets.getJSONObject(key);
            String hash = obj.getString("hash");
            String subDir = hash.substring(0, 2);
            File target = new File(DOT_MINECRAFT, "assets/objects/" + subDir + "/" + hash);
            if (!target.exists()) {
                target.getParentFile().mkdirs();
                String assetUrl = "https://resources.download.minecraft.net/" + subDir + "/" + hash;
                downloadFile(assetUrl, target);
            }
            index++;
            double progress = 0.7 + (0.2 * (index / (double) total));
            callback.onProgress(progress, I18n.get("assets.path") + key);
        }
    }

    // === EXTRACT NATIVES ===
    private static void extractNatives(String version) throws Exception {
        File versionJson = new File(DOT_MINECRAFT, "versions/" + version + "/" + version + ".json");
        JSONObject json = new JSONObject(Files.readString(versionJson.toPath()));
        JSONArray libs = json.getJSONArray("libraries");

        File nativesDir = new File(DOT_MINECRAFT, "versions/" + version + "/natives");
        nativesDir.mkdirs();

        String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        String classifier = osName.contains("win") ? "natives-windows" :
                           (osName.contains("mac") ? "natives-macos" : "natives-linux");

        for (int i = 0; i < libs.length(); i++) {
            JSONObject lib = libs.getJSONObject(i);
            if (!lib.has("downloads")) continue;

            JSONObject classifiers = lib.getJSONObject("downloads").optJSONObject("classifiers");
            if (classifiers == null) continue;
            JSONObject nativeObj = classifiers.optJSONObject(classifier);
            if (nativeObj == null) continue;

            String url = nativeObj.getString("url");
            String path = nativeObj.getString("path");
            File nativeJar = new File(DOT_MINECRAFT, "libraries/" + path);

            if (!nativeJar.exists()) {
                nativeJar.getParentFile().mkdirs();
                downloadFile(url, nativeJar);
            }

            try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(new FileInputStream(nativeJar))) {
                java.util.zip.ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.isDirectory() || entry.getName().contains("META-INF")) continue;
                    File outFile = new File(nativesDir, entry.getName());
                    outFile.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(outFile)) {
                        zis.transferTo(fos);
                    }
                }
            }
        }
    }

    // === GET CLASSPATH ===
    private static String getClasspath(String version) throws Exception {
        File versionJson = new File(DOT_MINECRAFT, "versions/" + version + "/" + version + ".json");
        JSONObject json = new JSONObject(Files.readString(versionJson.toPath()));

        List<String> cp = new ArrayList<>();
        cp.add(new File(DOT_MINECRAFT, "versions/" + version + "/" + version + ".jar").getAbsolutePath());

        JSONArray libs = json.getJSONArray("libraries");
        for (int i = 0; i < libs.length(); i++) {
            JSONObject lib = libs.getJSONObject(i);
            if (!lib.has("downloads")) continue;
            JSONObject artifact = lib.getJSONObject("downloads").optJSONObject("artifact");
            if (artifact == null) continue;
            String path = artifact.getString("path");
            cp.add(new File(DOT_MINECRAFT, "libraries/" + path).getAbsolutePath());
        }
        return String.join(";", cp);
    }

    // === HELPER DOWNLOAD ===
    private static void downloadFile(String url, File target) throws Exception {
        try (InputStream in = new URL(url).openStream()) {
            target.getParentFile().mkdirs();
            Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String readURL(String url) throws Exception {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new URL(url).openStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }
}
