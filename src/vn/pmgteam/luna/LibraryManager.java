package vn.pmgteam.luna;

import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;

public class LibraryManager {
    private final File librariesDir;

    public LibraryManager(File dotMinecraft) {
        this.librariesDir = new File(dotMinecraft, "libraries");
    }

    public void downloadLibraries(JSONArray libraries) throws IOException {
        librariesDir.mkdirs();
        for (int i = 0; i < libraries.length(); i++) {
            JSONObject lib = libraries.getJSONObject(i);

            if (lib.has("rules")) {
                if (!checkRules(lib.getJSONArray("rules"))) continue;
            }

            JSONObject downloads = lib.optJSONObject("downloads");
            if (downloads == null) continue;

            JSONObject artifact = downloads.optJSONObject("artifact");
            if (artifact != null) {
                downloadFile(artifact);
            }

            JSONObject classifiers = downloads.optJSONObject("classifiers");
            if (classifiers != null) {
                for (String key : classifiers.keySet()) {
                    downloadFile(classifiers.getJSONObject(key));
                }
            }
        }
    }

    private boolean checkRules(JSONArray rules) {
        String osName = System.getProperty("os.name").toLowerCase();
        boolean allowed = true;
        for (int i = 0; i < rules.length(); i++) {
            JSONObject rule = rules.getJSONObject(i);
            String action = rule.getString("action");
            JSONObject os = rule.optJSONObject("os");
            if (os != null) {
                String name = os.optString("name");
                if (osName.contains(name)) {
                    allowed = action.equals("allow");
                }
            } else {
                allowed = action.equals("allow");
            }
        }
        return allowed;
    }

    private void downloadFile(JSONObject artifact) throws IOException {
        String url = artifact.getString("url");
        String path = artifact.getString("path");
        File outFile = new File(librariesDir, path);

        if (outFile.exists()) return;

        outFile.getParentFile().mkdirs();
        try (InputStream in = new URL(url).openStream()) {
            Files.copy(in, outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Downloaded " + path);
        }
    }
}
