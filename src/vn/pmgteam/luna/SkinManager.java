package vn.pmgteam.luna;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.geometry.Pos;

import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.json.*;

public class SkinManager {

    private static final File SKINS_DIR = new File(System.getProperty("user.home"), ".luna/skins");
    private static final File MAPPING_JSON = new File(SKINS_DIR, "skin_mapping.json");

    private Map<String, JSONObject> uuidToSkin = new HashMap<>();

    public SkinManager() {
        SKINS_DIR.mkdirs();
        loadMapping();
    }

    // ===================== MAPPING =====================
    private void loadMapping() {
        if (!MAPPING_JSON.exists()) return;
        try {
            String content = Files.readString(MAPPING_JSON.toPath());
            JSONObject obj = new JSONObject(content);
            for (String uuid : obj.keySet()) {
                uuidToSkin.put(uuid, obj.getJSONObject(uuid));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveMapping() {
        try {
            JSONObject obj = new JSONObject();
            for (Map.Entry<String, JSONObject> entry : uuidToSkin.entrySet()) {
                obj.put(entry.getKey(), entry.getValue());
            }
            Files.writeString(MAPPING_JSON.toPath(), obj.toString(4), StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ===================== UPLOAD SKIN =====================
    public void uploadSkin(File pngFile, String uuid, String variant) {
        try {
            // Hash PNG content
            byte[] bytes = Files.readAllBytes(pngFile.toPath());
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] hash = sha1.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            String timestamp = String.valueOf(System.currentTimeMillis());
            String filename = sb.toString() + "-" + timestamp + ".png";

            // Copy PNG
            File target = new File(SKINS_DIR, filename);
            Files.copy(pngFile.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);

            // Update mapping
            JSONObject skinObj = new JSONObject();
            skinObj.put("file", filename);
            skinObj.put("variant", variant);
            uuidToSkin.put(uuid, skinObj);
            saveMapping();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ===================== PREVIEW 2D =====================
    public VBox buildSkinPreview(File pngFile) {
        VBox box = new VBox(5);
        box.setAlignment(Pos.CENTER);
        try {
            Image img = new Image(pngFile.toURI().toString());

            ImageView front = buildMinecraftChar2DFront(img);
            front.setFitWidth(64 * 2);
            front.setFitHeight(128 * 2);
            front.setPreserveRatio(true);

            ImageView back = buildMinecraftChar2DBack(img);
            back.setFitWidth(64 * 2);
            back.setFitHeight(128 * 2);
            back.setPreserveRatio(true);

            HBox previews = new HBox(10, front, back);
            previews.setAlignment(Pos.CENTER);

            box.getChildren().addAll(previews, new Label(pngFile.getName()));

        } catch (Exception e) {
            e.printStackTrace();
        }
        return box;
    }

    // ===================== OFFLINE INJECTION =====================
    public void injectSkinCommandline(List<String> cmd, String uuid) {
        JSONObject skinObj = uuidToSkin.get(uuid);
        if (skinObj == null) return;

        File skinDir = SKINS_DIR;
        cmd.add("--offlineSkinDir");
        cmd.add(skinDir.getAbsolutePath());
        cmd.add("--offlineSkinUUID");
        cmd.add(uuid);
    }

    // ===================== HELPER 2D BUILD =====================
    private ImageView buildMinecraftChar2DFront(Image skin) {
        ImageView iv = new ImageView(skin);
        iv.setViewport(new javafx.geometry.Rectangle2D(8, 8, 8, 8)); // chỉ ví dụ phần mặt
        return iv;
    }

    private ImageView buildMinecraftChar2DBack(Image skin) {
        ImageView iv = new ImageView(skin);
        iv.setViewport(new javafx.geometry.Rectangle2D(40, 8, 8, 8)); // chỉ ví dụ phần mặt phía sau
        return iv;
    }

    // ===================== LOAD ALL SKINS =====================
    public void loadSkins2D(FlowPane container) {
        container.getChildren().clear();
        if (!SKINS_DIR.exists()) return;

        for (File skinFile : SKINS_DIR.listFiles(f -> f.getName().endsWith(".png"))) {
            try {
                VBox previewBox = buildSkinPreview(skinFile);
                container.getChildren().add(previewBox);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
