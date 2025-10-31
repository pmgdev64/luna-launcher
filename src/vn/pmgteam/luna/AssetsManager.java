package vn.pmgteam.luna;

import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;

public class AssetsManager {
    private final File assetsDir;

    public AssetsManager(File dotMinecraft) {
        this.assetsDir = new File(dotMinecraft, "assets");
    }

    public void downloadAssets(JSONObject assetIndex) throws IOException {
        String id = assetIndex.getString("id");
        String url = assetIndex.getJSONObject("url").toString();
        File indexesDir = new File(assetsDir, "indexes");
        File objectsDir = new File(assetsDir, "objects");
        indexesDir.mkdirs();
        objectsDir.mkdirs();

        File indexFile = new File(indexesDir, id + ".json");
        if (!indexFile.exists()) {
            try (InputStream in = new URL(assetIndex.getString("url")).openStream()) {
                Files.copy(in, indexFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }

        JSONObject indexJson = new JSONObject(Files.readString(indexFile.toPath()));
        JSONObject objects = indexJson.getJSONObject("objects");
        for (String key : objects.keySet()) {
            JSONObject obj = objects.getJSONObject(key);
            String hash = obj.getString("hash");
            String sub = hash.substring(0, 2);
            File outFile = new File(objectsDir, sub + "/" + hash);
            if (outFile.exists()) continue;

            outFile.getParentFile().mkdirs();
            String objUrl = "https://resources.download.minecraft.net/" + sub + "/" + hash;
            try (InputStream in = new URL(objUrl).openStream()) {
                Files.copy(in, outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Downloaded asset " + key);
            }
        }
    }
}
