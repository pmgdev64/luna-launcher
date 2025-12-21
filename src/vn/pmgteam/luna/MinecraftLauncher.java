package vn.pmgteam.luna;

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
    public static LauncherConfig cfg;

    public record Session(String username, String accessToken, String uuid) {}
    public interface ProgressCallback { void onProgress(double progress, String status); }

    public static LauncherConfig launcherConfig = new LauncherConfig();

    // === FETCH AVAILABLE VERSIONS ===
    public static List<String> fetchAvailableVersions() {
        List<String> versions = new ArrayList<>();
        try {
            String json = readURL(MANIFEST_URL);
            JSONObject manifest = new JSONObject(json);
            JSONArray arr = manifest.getJSONArray("versions");
            for (int i = 0; i < arr.length(); i++)
                versions.add(arr.getJSONObject(i).getString("id"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return versions;
    }

    // === CHECK INSTALLED VERSION ===
    public static boolean isVersionInstalled(String version) {
        File vDir = new File(DOT_MINECRAFT, "versions/" + version);
        return new File(vDir, version + ".json").exists() && new File(vDir, version + ".jar").exists();
    }

    // === LEGACY DETECTION ===
    public static boolean isLegacyVersion(String version) {
        String v = version.toLowerCase(Locale.ROOT);
        return v.startsWith("a") || v.startsWith("b") || v.startsWith("inf") || v.startsWith("c") || v.startsWith("rd");
    }

    // === CHỌN JAVA PATH ===
    private static String selectJavaPath(String version) {
        return "C:\\Program Files\\Java\\jre1.8.0_202\\bin\\java.exe";
    }

    // === MAIN LAUNCH LOGIC ===
    public static void launch(String version, Session session, ProgressCallback callback) throws Exception {
        File vDir = new File(DOT_MINECRAFT, "versions/" + version);
        File vJson = new File(vDir, version + ".json");
        File vJar  = new File(vDir, version + ".jar");
        String javaPath = selectJavaPath(version);

        if (isLegacyVersion(version)) {
            if (!vJar.exists()) throw new IOException("Missing legacy jar: " + vJar);

            callback.onProgress(0.1, "Checking legacy resources...");
            downloadResource(version, callback);

            callback.onProgress(1.0, "Launching legacy...");
            List<String> cmd = new ArrayList<>();
            cmd.add(javaPath);
            cmd.add("-Xmx512M");
            cmd.add("-cp");
            cmd.add(vJar.getAbsolutePath());
            cmd.add(version.startsWith("rd") ? "com.mojang.rubydung.RubyDung" : "com.mojang.minecraft.Minecraft");
            cmd.add(session.username());

            cfg.lastCommandLine = new ArrayList<>(cmd);
            try {
				Main.configManager.save();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

            new ProcessBuilder(cmd).directory(DOT_MINECRAFT).inheritIO().start();
            return;
        }

        if (!vJson.exists()) { callback.onProgress(0, "Downloading version JSON..."); downloadVersionJson(version); }
        if (!vJar.exists())  { callback.onProgress(0.1, "Downloading client jar..."); downloadClientJar(version); }

        callback.onProgress(0.3, "Downloading libraries...");
        downloadLibraries(version, callback);

        callback.onProgress(0.7, "Downloading assets...");
        downloadAssets(version, callback);

        callback.onProgress(0.9, "Extracting natives...");
        extractNatives(version);
        
        callback.onProgress(0.95, I18n.get("icons.checking"));
        downloadIconsIfMissing(version);


        if (version.startsWith("1.6") || version.startsWith("1.7")) {
            callback.onProgress(0.92, "Downloading legacy sounds...");
            downloadResource(version, callback);
        }

        JSONObject vData = new JSONObject(Files.readString(vJson.toPath()));
        String mainClass = vData.optString("mainClass", "net.minecraft.client.main.Main");

        List<String> cmd = new ArrayList<>();
        cmd.add(javaPath);
        cmd.add("-Xmx2G");
        cmd.add("-Xms1G");

        File nativesDir = new File(vDir, "natives");
        cmd.add("-Djava.library.path=" + nativesDir.getAbsolutePath());
        cmd.add("-Dorg.lwjgl.librarypath=" + nativesDir.getAbsolutePath());

        cmd.add("-cp");
        cmd.add(getClasspath(version));
        cmd.add(mainClass);

        cmd.add("--username");      cmd.add(session.username());
        cmd.add("--version");       cmd.add(version);
        cmd.add("--gameDir");       cmd.add(DOT_MINECRAFT.getAbsolutePath());
        cmd.add("--assetsDir");     cmd.add(new File(DOT_MINECRAFT, "assets").getAbsolutePath());
        cmd.add("--accessToken");   cmd.add(Optional.ofNullable(session.accessToken()).orElse("0"));
        cmd.add("--uuid");          cmd.add(Optional.ofNullable(session.uuid()).orElse("0"));
        cmd.add("--userProperties");cmd.add("{}");

        cfg.lastCommandLine = new ArrayList<>(cmd);
        Main.configManager.save();

        callback.onProgress(1.0, "Launching game...");
        new ProcessBuilder(cmd).directory(DOT_MINECRAFT).inheritIO().start();
        Main.logInstance.info("Minecraft Args: " + cmd);
    }

    // === DOWNLOAD RESOURCE (sound + icon only) ===
    public static void downloadResource(String version, ProgressCallback callback) {
        callback.onProgress(0, "Preparing resources for " + version + "...");
        boolean legacy = version.startsWith("1.0") || version.startsWith("b") || version.startsWith("a") || version.startsWith("1.6") || version.startsWith("1.7");

        File baseDir = new File(DOT_MINECRAFT, legacy ? "resources" : "assets/minecraft");
        String baseUrl = "https://mcasset.cloud/" + version + "/assets/minecraft/";

        String[] resourceFiles = {
            "newsound/random/click.ogg", "newsound/dig/stone1.ogg", "newsound/dig/grass1.ogg",
            "newsound/step/wood1.ogg", "newsound/step/stone1.ogg",
            "sounds/random/click.ogg", "sounds/dig/stone1.ogg", "sounds/step/wood1.ogg",
            "textures/gui/icons.png", "textures/items/apple.png"
        };

        int total = resourceFiles.length, done = 0;
        for (String path : resourceFiles) {
            try {
                File out = new File(baseDir, path);
                if (out.exists() && out.length() > 1024) {
                    done++; callback.onProgress((double) done / total, "Skipped " + path);
                    continue;
                }
                out.getParentFile().mkdirs();
                URL url = new URL(baseUrl + path);
                try (InputStream in = url.openStream()) {
                    Files.copy(in, out.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("⬇️ " + path);
                } catch (FileNotFoundException nf) {
                    System.err.println("⚠️ Missing on server: " + path);
                }
                done++; callback.onProgress((double) done / total, "Processed " + path);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        callback.onProgress(1.0, "✅ Resource download for " + version + " completed!");
    }

    // === DOWNLOAD VERSION JSON ===
    private static void downloadVersionJson(String version) throws Exception {
        JSONObject manifest = new JSONObject(readURL(MANIFEST_URL));
        JSONArray versions = manifest.getJSONArray("versions");
        String url = null;
        for (int i = 0; i < versions.length(); i++) {
            JSONObject obj = versions.getJSONObject(i);
            if (obj.getString("id").equals(version)) url = obj.getString("url");
        }
        if (url == null) throw new IOException("Version not found: " + version);
        String json = readURL(url);
        File dir = new File(DOT_MINECRAFT, "versions/" + version);
        dir.mkdirs();
        Files.writeString(new File(dir, version + ".json").toPath(), json);
    }

    private static void downloadClientJar(String version) throws Exception {
        File jsonFile = new File(DOT_MINECRAFT, "versions/" + version + "/" + version + ".json");
        JSONObject json = new JSONObject(Files.readString(jsonFile.toPath()));
        JSONObject downloads = json.getJSONObject("downloads").getJSONObject("client");
        downloadFile(downloads.getString("url"), new File(DOT_MINECRAFT, "versions/" + version + "/" + version + ".jar"));
    }

    private static void downloadLibraries(String version, ProgressCallback callback) throws Exception {
        File jsonFile = new File(DOT_MINECRAFT, "versions/" + version + "/" + version + ".json");
        JSONObject json = new JSONObject(Files.readString(jsonFile.toPath()));
        JSONArray libs = json.getJSONArray("libraries");
        int total = libs.length();
        for (int i = 0; i < total; i++) {
            JSONObject lib = libs.getJSONObject(i);
            JSONObject artifact = lib.optJSONObject("downloads").optJSONObject("artifact");
            if (artifact == null) continue;
            File file = new File(DOT_MINECRAFT, "libraries/" + artifact.getString("path"));
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                downloadFile(artifact.getString("url"), file);
            }
            callback.onProgress(0.3 + 0.4 * ((i + 1) / (double) total), artifact.getString("path"));
        }
    }

    private static void downloadAssets(String version, ProgressCallback callback) throws Exception {
        File jsonFile = new File(DOT_MINECRAFT, "versions/" + version + "/" + version + ".json");
        JSONObject json = new JSONObject(Files.readString(jsonFile.toPath()));
        JSONObject assetIndex = json.getJSONObject("assetIndex");
        JSONObject objects = new JSONObject(readURL(assetIndex.getString("url"))).getJSONObject("objects");
        int total = objects.length(), index = 0;
        for (String key : objects.keySet()) {
            JSONObject obj = objects.getJSONObject(key);
            String hash = obj.getString("hash"), sub = hash.substring(0, 2);
            File target = new File(DOT_MINECRAFT, "assets/objects/" + sub + "/" + hash);
            if (!target.exists()) {
                target.getParentFile().mkdirs();
                downloadFile("https://resources.download.minecraft.net/" + sub + "/" + hash, target);
            }
            index++;
            callback.onProgress(0.7 + 0.2 * (index / (double) total), key);
        }
    }

    private static void extractNatives(String version) throws Exception {
        File jsonFile = new File(DOT_MINECRAFT, "versions/" + version + "/" + version + ".json");
        JSONObject json = new JSONObject(Files.readString(jsonFile.toPath()));
        JSONArray libs = json.getJSONArray("libraries");
        File nativesDir = new File(DOT_MINECRAFT, "versions/" + version + "/natives");
        nativesDir.mkdirs();

        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        String classifier = os.contains("win") ? "natives-windows" :
                            os.contains("mac") ? "natives-macos" : "natives-linux";

        for (int i = 0; i < libs.length(); i++) {
            JSONObject lib = libs.getJSONObject(i);
            JSONObject cls = lib.optJSONObject("downloads").optJSONObject("classifiers");
            if (cls == null) continue;
            JSONObject nativeObj = cls.optJSONObject(classifier);
            if (nativeObj == null) continue;

            File nativeJar = new File(DOT_MINECRAFT, "libraries/" + nativeObj.getString("path"));
            if (!nativeJar.exists()) {
                nativeJar.getParentFile().mkdirs();
                downloadFile(nativeObj.getString("url"), nativeJar);
            }

            try (var zis = new java.util.zip.ZipInputStream(new FileInputStream(nativeJar))) {
                java.util.zip.ZipEntry e;
                while ((e = zis.getNextEntry()) != null) {
                    if (e.isDirectory() || e.getName().contains("META-INF")) continue;
                    File out = new File(nativesDir, e.getName());
                    out.getParentFile().mkdirs();
                    try 
                    {
                    	FileOutputStream fos = new FileOutputStream(out);
                    	zis.transferTo(fos);
                    }
                    catch (Exception ex1)
                    {
                    	ex1.printStackTrace();
                    }
                }
            }
        }
    }
    
    private static void downloadIconsIfMissing(String selectedVersion) {
        try {
            File iconsDir = new File(DOT_MINECRAFT, "assets/icons");
            if (!iconsDir.exists()) iconsDir.mkdirs();

            File icon16 = new File(iconsDir, "icon_16x16.png");
            File icon32 = new File(iconsDir, "icon_32x32.png");

            // Nếu thiếu thì tải lại
            if (!icon16.exists()) {
                System.out.println("Downloading missing icon_16x16.png...");
                downloadFile(
                    "https://mcasset.cloud/"+ selectedVersion +"/assets/icons/icon_16x16.png",
                    icon16
                );
            }
            if (!icon32.exists()) {
                System.out.println("Downloading missing icon_32x32.png...");
                downloadFile(
                	"https://mcasset.cloud/"+ selectedVersion +"/assets/icons/icon_16x16.png",
                    icon32
                );
            }
        } catch (Exception e) {
            System.err.println("Failed to ensure icons: " + e.getMessage());
        }
    }


    private static String getClasspath(String version) throws Exception {
        File jsonFile = new File(DOT_MINECRAFT, "versions/" + version + "/" + version + ".json");
        JSONObject json = new JSONObject(Files.readString(jsonFile.toPath()));
        List<String> cp = new ArrayList<>();
        cp.add(new File(DOT_MINECRAFT, "versions/" + version + "/" + version + ".jar").getAbsolutePath());
        JSONArray libs = json.getJSONArray("libraries");
        for (int i = 0; i < libs.length(); i++) {
            JSONObject artifact = libs.getJSONObject(i).optJSONObject("downloads").optJSONObject("artifact");
            if (artifact != null)
                cp.add(new File(DOT_MINECRAFT, "libraries/" + artifact.getString("path")).getAbsolutePath());
        }
        return String.join(";", cp);
    }

    private static void downloadFile(String url, File target) throws Exception {
        try (InputStream in = new URL(url).openStream()) {
            target.getParentFile().mkdirs();
            Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String readURL(String url) throws Exception {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new URL(url).openStream()))) {
            StringBuilder sb = new StringBuilder();
            String line; while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }
}
