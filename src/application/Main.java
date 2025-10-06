package application;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.CacheHint;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.*;
import javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.stage.FileChooser;
import javafx.stage.Popup;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import vn.pmgteam.kclient.ConfigManager;
import vn.pmgteam.kclient.I18n;
import vn.pmgteam.kclient.LauncherConfig;
import vn.pmgteam.kclient.LoadingGui;
import vn.pmgteam.kclient.LunarCalendar;
import vn.pmgteam.kclient.MessageBox;
import vn.pmgteam.kclient.OpenFileBox;
import vn.pmgteam.kclient.UserProfile;
import vn.pmgteam.kclient.LunarDate;
import vn.pmgteam.kclient.MinecraftLauncher;
import vn.pmgteam.kclient.MinecraftSkinViewer;
import vn.pmgteam.kclient.NofiticationBox;
import vn.pmgteam.kclient.auth.AuthManager;
import vn.pmgteam.kclient.auth.LoginAuthBox;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.function.Function;

import javax.imageio.ImageIO;

import org.json.JSONArray;
import org.json.JSONObject;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import com.luciad.imageio.webp.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class Main extends Application {

    private Node currentPage;

    private ComboBox<String> versionSelect; // chỉ khai báo
    public static Image titleIcon; // chỉ khai báo
    
    private ConfigManager configManager;
    private LauncherConfig cfg;
    
    public static Image folderIcon;
    
    public static String userHome = System.getProperty("user.home");
    
    public static String defaultGamePath = userHome + "\\Appdata\\Roaming\\.minecraft";
    
    public String selectedGamePath;
    
    public MinecraftLauncher minecraftLauncher = new MinecraftLauncher();
    
    public static String lunaDir = userHome + "/Appdata/Roaming/.luna";
    
    private final List<Popup> activePopups = new ArrayList<>();
    private final double baseX = 980; // góc phải (có thể chỉnh theo màn hình)
    private final double baseY = 660; // góc dưới
    private final double spacing = 0.0; // khoảng cách giữa các popup
    
    private static boolean debugMode = false;
    private static boolean verbose = false;
    private static boolean noSplash = false;
    
    // Ở đầu class Main, thêm:
    private StackPane overlay;
    
    public static Logger logInstance = LogManager.getLogger("LunaLauncher");
    
    //public FlowPane skinsContainer;

    
    private boolean isVersionInstalled(String version) {
        File jar = new File(MinecraftLauncher.DOT_MINECRAFT, "versions/" + version + "/" + version + ".jar");
        File json = new File(MinecraftLauncher.DOT_MINECRAFT, "versions/" + version + "/" + version + ".json");
        return jar.exists() && json.exists();
    }
    
    private void showOverlay() {
        overlay.setVisible(true);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), overlay);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
    }

    private void hideOverlay() {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), overlay);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> overlay.setVisible(false));
        fadeOut.play();
    }
    
    private static final Interpolator CUBIC_OUT = new Interpolator() {
        @Override
        protected double curve(double t) {
            // cubic ease out: starts fast, ends slow
            return 1 - Math.pow(1 - t, 3);
        }
    };
    
    private Image loadImage(String url) {
        try {
            if (url.endsWith(".webp")) {
                InputStream in = new URL(url).openStream();
                BufferedImage buffered = ImageIO.read(in); // WebP plugin phải được cài
                return SwingFXUtils.toFXImage(buffered, null);
            } else {
                return new Image(url, true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new Image(getClass().getResource("/resources/question_mark.png").toExternalForm());
        }
    }
    
    private static JSONArray fetchModsFromModrinth(String query) throws Exception {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String apiUrl = "https://api.modrinth.com/v2/search?query=" + encodedQuery + "&limit=20&offset=0";
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "JavaFX-ModLauncher");

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) throw new IOException("Server returned HTTP response code: " + responseCode);

        Scanner sc = new Scanner(conn.getInputStream()).useDelimiter("\\A");
        String response = sc.hasNext() ? sc.next() : "";
        JSONObject json = new JSONObject(response);

        // --- In toàn bộ JSON search trả về ---
        System.out.println("=== Modrinth search JSON ===");
        System.out.println(json.toString(4)); // 4 là indent để đọc đẹp hơn

        JSONArray hits = json.getJSONArray("hits");
        JSONArray mods = new JSONArray();

        for (int i = 0; i < hits.length(); i++) {
            JSONObject mod = hits.getJSONObject(i);
            JSONObject modObj = new JSONObject();

            modObj.put("title", mod.optString("title", mod.optString("slug", "Unknown")));
            modObj.put("description", mod.optString("description", "No description"));
            modObj.put("downloads", mod.optInt("downloads", 0));
            modObj.put("icon_url", mod.optString("icon_url", ""));

            String authorName = "Unknown Author";
            String authorAvatar = "";

            String authorId = null;
            if (mod.has("authors") && mod.getJSONArray("authors").length() > 0) {
                authorId = mod.getJSONArray("authors").getString(0);
                try {
                    JSONObject user = fetchUser(authorId);

                    // --- In JSON author ---
                    System.out.println("=== Author JSON ===");
                    System.out.println(user.toString(4));

                    authorName = user.optString("username", "Unknown Author");
                    authorAvatar = user.optString("avatar_url", "");
                } catch (Exception e) {
                    authorName = "Unknown Author";
                    authorAvatar = "";
                }
            } else if (mod.has("team") && !mod.optString("team").isEmpty()) {
                authorId = mod.optString("team");
                try {
                    JSONObject team = fetchTeam(authorId);

                    // --- In JSON team ---
                    System.out.println("=== Team JSON ===");
                    System.out.println(team.toString(4));

                    authorName = team.optString("name", "Unknown Author");
                    authorAvatar = "";
                } catch (Exception e) {
                    authorName = "Unknown Author";
                    authorAvatar = "";
                }
            }

            modObj.put("author_name", authorName);
            modObj.put("author_avatar", authorAvatar);
            mods.put(modObj);
        }

        return mods;
    }

    /**
     * Gọi API /v2/user/{id} để lấy thông tin người dùng
     */
    private static JSONObject fetchUser(String userId) throws Exception {
        URL url = new URL("https://api.modrinth.com/v2/user/" + userId);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "JavaFX-ModLauncher");

        Scanner sc = new Scanner(conn.getInputStream()).useDelimiter("\\A");
        String response = sc.hasNext() ? sc.next() : "";
        return new JSONObject(response);
    }

    /**
     * Gọi API /v2/team/{id} để lấy thông tin team
     */
    private static JSONObject fetchTeam(String teamId) throws Exception {
        URL url = new URL("https://api.modrinth.com/v2/team/" + teamId);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "JavaFX-ModLauncher");

        Scanner sc = new Scanner(conn.getInputStream()).useDelimiter("\\A");
        String response = sc.hasNext() ? sc.next() : "";
        return new JSONObject(response);
    }
    
    // Fetch từ pmgdev64.github.io/api/<các phần còn lại>
    private static JSONObject fetchJsonFromApi(String endpoint) throws Exception {
        String url = "https://pmgdev64.github.io/api/" + endpoint;
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "JavaFX-Launcher");

        try (InputStream in = conn.getInputStream();
             Scanner sc = new Scanner(in).useDelimiter("\\A")) {
            String response = sc.hasNext() ? sc.next() : "";
            return new JSONObject(response);
        }
    }
    
    private static void downloadMod(String projectId, String versionId) throws IOException {
        // Modrinth API link tải file mod: 
        // https://api.modrinth.com/v2/project/{projectId}/version/{versionId}
        String versionUrl = "https://api.modrinth.com/v2/version/" + versionId;
        JSONObject versionJson;
        try (Scanner sc = new Scanner(new URL(versionUrl).openStream()).useDelimiter("\\A")) {
            String response = sc.hasNext() ? sc.next() : "";
            versionJson = new JSONObject(response);
        }

        JSONArray files = versionJson.getJSONArray("files");
        if (files.length() == 0) throw new IOException("No files found for this mod version.");

        JSONObject file = files.getJSONObject(0);
        String fileUrl = file.getString("url");
        String fileName = file.getString("filename");

        // Tạo thư mục mods nếu chưa có
        Path modsDir = Paths.get(System.getProperty("user.home"), "\\AppData\\Roaming\\.minecraft", "mods");
        if (!Files.exists(modsDir)) {
            Files.createDirectories(modsDir);
        }


        // Download file mod
        try (InputStream in = new URL(fileUrl).openStream()) {
            Files.copy(in, modsDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
        }

        System.out.println("Mod " + fileName + " downloaded to mods/");
    }


    // === Load tất cả skins, toggle front/back ===
    public void loadSkins2D(FlowPane container) {
        container.getChildren().clear();
        File skinsDir = new File(System.getProperty("user.home"), "AppData/Roaming/.luna/skins");
        if (!skinsDir.exists()) return;

        for (File skinFile : skinsDir.listFiles(f -> f.getName().endsWith(".png"))) {
            try {
                Image skin = new Image(skinFile.toURI().toString());

                ImageView front = buildMinecraftChar2DFront(skin);
                front.setFitWidth(64 * 2);
                front.setFitHeight(128 * 2);
                front.setPreserveRatio(true);

                ImageView back = buildMinecraftChar2DBack(skin);
                back.setFitWidth(64 * 2);
                back.setFitHeight(128 * 2);
                back.setPreserveRatio(true);

                HBox previews = new HBox(10, front, back);
                previews.setAlignment(Pos.CENTER);

                VBox skinBox = new VBox(5, previews, new Label(skinFile.getName()));
                skinBox.setAlignment(Pos.CENTER);

                container.getChildren().add(skinBox);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }


    // === Render mặt trước ===
    public ImageView buildMinecraftChar2DFront(Image skin) {
        int scale = 4;
        int w = 16, h = 32;
        WritableImage canvas = new WritableImage(w * scale, h * scale);
        PixelReader r = skin.getPixelReader();
        PixelWriter wri = canvas.getPixelWriter();

        // Head (front 8x8 at 8,8)
        copyRegion(r, wri, 8, 8, 8, 8, 4, 0, scale);

        // Body (front 8x12 at 20,20)
        copyRegion(r, wri, 20, 20, 8, 12, 4, 8, scale);

        // Right Arm (front 4x12 at 44,20)
        copyRegion(r, wri, 44, 20, 4, 12, 12, 8, scale);

        // Left Arm (mirror: 36,52 nếu skin 64x64, fallback 44,20)
        copyRegion(r, wri, 36, 52, 4, 12, 0, 8, scale);

        // Right Leg (front 4x12 at 4,20)
        copyRegion(r, wri, 4, 20, 4, 12, 8, 20, scale);

        // Left Leg (front 4x12 at 20,52 nếu 64x64, fallback 4,20)
        copyRegion(r, wri, 20, 52, 4, 12, 4, 20, scale);

        ImageView iv = new ImageView(canvas);
        iv.setSmooth(false);
        return iv;
    }

    // === Render mặt sau ===
    public ImageView buildMinecraftChar2DBack(Image skin) {
        int scale = 4;
        int w = 16, h = 32;
        WritableImage canvas = new WritableImage(w * scale, h * scale);
        PixelReader r = skin.getPixelReader();
        PixelWriter wri = canvas.getPixelWriter();

        // Head (back 8x8 at 24,8)
        copyRegion(r, wri, 24, 8, 8, 8, 4, 0, scale);

        // Body (back 8x12 at 32,20)
        copyRegion(r, wri, 32, 20, 8, 12, 4, 8, scale);

        // Right Arm back (4x12 at 52,20)
        copyRegion(r, wri, 52, 20, 4, 12, 12, 8, scale);

        // Left Arm back (4x12 at 44,52 nếu 64x64)
        copyRegion(r, wri, 44, 52, 4, 12, 0, 8, scale);

        // Right Leg back (4x12 at 12,20)
        copyRegion(r, wri, 12, 20, 4, 12, 8, 20, scale);

        // Left Leg back (4x12 at 28,52 nếu 64x64)
        copyRegion(r, wri, 28, 52, 4, 12, 4, 20, scale);

        ImageView iv = new ImageView(canvas);
        iv.setSmooth(false);
        return iv;
    }

    // === Helper copy ===
    private void copyRegion(PixelReader r, PixelWriter wri,
                            int sx, int sy, int sw, int sh,
                            int dx, int dy, int scale) {
        for (int y = 0; y < sh; y++)
            for (int x = 0; x < sw; x++) {
                Color c = r.getColor(sx + x, sy + y);
                for (int dy2 = 0; dy2 < scale; dy2++)
                    for (int dx2 = 0; dx2 < scale; dx2++)
                        wri.setColor((dx + x) * scale + dx2,
                                     (dy + y) * scale + dy2, c);
            }
    }
    
    public static void apply(ComboBox<?> comboBox) {
        comboBox.showingProperty().addListener((obs, wasShowing, isNowShowing) -> {
            if (isNowShowing) {
                Platform.runLater(() -> {
                    if (comboBox.getSkin() instanceof ComboBoxListViewSkin<?> skin) {
                        ListView<?> listView = (ListView<?>) skin.getPopupContent();
                        Node popup = listView;

                        popup.setOpacity(0);
                        popup.setTranslateY(-15);
                        popup.setScaleY(0.9);

                        Timeline openAnim = new Timeline(
                            new KeyFrame(Duration.ZERO,
                                new KeyValue(popup.opacityProperty(), 0),
                                new KeyValue(popup.translateYProperty(), -15),
                                new KeyValue(popup.scaleYProperty(), 0.9)
                            ),
                            new KeyFrame(Duration.millis(360),
                                new KeyValue(popup.opacityProperty(), 1, CUBIC_OUT),
                                new KeyValue(popup.translateYProperty(), 0, CUBIC_OUT),
                                new KeyValue(popup.scaleYProperty(), 1, CUBIC_OUT)
                            )
                        );
                        openAnim.play();
                    }
                });
            } else {
                // Khi ComboBox đóng lại, thêm hiệu ứng trượt lên
                if (comboBox.getSkin() instanceof ComboBoxListViewSkin<?> skin) {
                    ListView<?> listView = (ListView<?>) skin.getPopupContent();
                    Node popup = listView;

                    Timeline closeAnim = new Timeline(
                        new KeyFrame(Duration.ZERO,
                            new KeyValue(popup.opacityProperty(), 1),
                            new KeyValue(popup.translateYProperty(), 0),
                            new KeyValue(popup.scaleYProperty(), 1)
                        ),
                        new KeyFrame(Duration.millis(240),
                            new KeyValue(popup.opacityProperty(), 0, CUBIC_OUT),
                            new KeyValue(popup.translateYProperty(), -10, CUBIC_OUT),
                            new KeyValue(popup.scaleYProperty(), 0.95, CUBIC_OUT)
                        )
                    );
                    closeAnim.play();
                }
            }
        });
    }


    
    @Override
    public void start(Stage stage) throws Exception {

    	I18n.load(getClass(), "luna", new Locale("vi"));
    	// Load config
    	configManager = new ConfigManager();
    	cfg = configManager.getConfig();
    	selectedGamePath = cfg.gamePath; // sử dụng gamePath đã lưu


    	// === Khởi tạo Image icon đúng cách ===
    	titleIcon = new Image(getClass().getResource("/resources/icons.png").toExternalForm());
    	stage.getIcons().add(titleIcon);

    	// === Layout chính ===
    	BorderPane layout = new BorderPane();
    	StackPane root = new StackPane(layout);

    	overlay = new StackPane();
    	overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.5);");
    	overlay.setVisible(false);

    	Label overlayLabel = new Label(I18n.get("overlay.load"));
    	overlayLabel.setTextFill(Color.WHITE);
    	overlayLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
    	overlay.getChildren().add(overlayLabel);

    	// === TOP: Window controls ===
    	HBox topBar = new HBox();
    	topBar.setPadding(new Insets(10));
    	topBar.setSpacing(10);
    	topBar.setAlignment(Pos.CENTER_RIGHT);

    	Button minimize = new Button(I18n.get("window.minimize"));
    	minimize.getStyleClass().add("window-control");
    	minimize.setOnAction(e -> stage.setIconified(true));

    	Button close = new Button(I18n.get("window.close"));
    	close.getStyleClass().add("window-control");
    	close.setOnAction(e -> {
    	    // ScaleTransition gốc
    	    ScaleTransition scale = new ScaleTransition(Duration.millis(350), root);
    	    scale.setToX(0);
    	    scale.setToY(0);
    	    scale.setInterpolator(Interpolator.EASE_IN);

    	    // FadeTransition gốc
    	    FadeTransition fade = new FadeTransition(Duration.millis(350), root);
    	    fade.setToValue(0);
    	    fade.setInterpolator(Interpolator.EASE_OUT);

    	    // TranslateTransition gốc
    	    TranslateTransition slide = new TranslateTransition(Duration.millis(350), root);
    	    slide.setToY(200);
    	    slide.setInterpolator(Interpolator.EASE_IN);

    	    // --------- Viền bo góc ---------
    	    Rectangle border = new Rectangle(root.getWidth(), root.getHeight());
    	    border.setArcWidth(20);   // bo góc X
    	    border.setArcHeight(20);  // bo góc Y
    	    border.setFill(Color.TRANSPARENT);
    	    border.setStroke(Color.DARKBLUE);
    	    border.setStrokeWidth(3);

    	    // Thêm vào root (hoặc overlay pane)
    	    root.getChildren().add(border);

    	    // Animate viền fade + shrink
    	    Timeline borderAnim = new Timeline(
    	        new KeyFrame(Duration.ZERO,
    	            new KeyValue(border.strokeWidthProperty(), 3),
    	            new KeyValue(border.opacityProperty(), 1)
    	        ),
    	        new KeyFrame(Duration.millis(300),
    	            new KeyValue(border.strokeWidthProperty(), 0),
    	            new KeyValue(border.opacityProperty(), 0)
    	        )
    	    );

    	    // --------- Kết hợp tất cả ---------
    	    ParallelTransition pt = new ParallelTransition(scale, fade, slide, borderAnim);
    	    pt.setOnFinished(ev -> stage.close());
    	    pt.play();
    	});

    	topBar.getChildren().addAll(minimize, close);
    	layout.setTop(topBar);

    	// === PAGES container (center) ===
    	StackPane pagesContainer = new StackPane();
    	pagesContainer.setPrefSize(800, 400);

    	// --- Home page ---
    	VBox homePage = new VBox(20);
    	homePage.setAlignment(Pos.CENTER);
    	homePage.setPadding(new Insets(20));

    	HBox playBox = new HBox(10);
    	playBox.setAlignment(Pos.CENTER);

    	Text playIcon = new Text("▶");
    	playIcon.setStyle("-fx-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");

    	Button playButton = new Button(I18n.get("menu.play"));
    	playButton.getStyleClass().add("play-button");

    	versionSelect = new ComboBox<>();
    	versionSelect.setItems(FXCollections.observableArrayList(minecraftLauncher.fetchAvailableVersions()));
    	if (cfg.lastVersion != null && versionSelect.getItems().contains(cfg.lastVersion)) {
    	    versionSelect.setValue(cfg.lastVersion); // Chọn version đã lưu
    	} else if (!versionSelect.getItems().isEmpty()) {
    	    versionSelect.setValue(versionSelect.getItems().get(0)); // Mặc định chọn version đầu tiên
    	}
    	versionSelect.setPromptText(I18n.get("menu.selectversion"));


    	versionSelect.setButtonCell(new ListCell<String>() {
    	    @Override
    	    protected void updateItem(String item, boolean empty) {
    	        super.updateItem(item, empty);
    	        if (empty || item == null) {
    	            setText(I18n.get("menu.selectversion"));
    	            setTextFill(Color.GRAY);
    	        } else {
    	            setText(item);
    	            setTextFill(Color.WHITE);
    	        }
    	    }
    	});

    	versionSelect.valueProperty().addListener((obs, oldVal, newVal) -> {
    		 if (newVal == null) return;

    		    // Cập nhật config
    		    cfg.lastVersion = newVal;
    		    configManager.save(); // ghi vào file config

    		    boolean installed = MinecraftLauncher.isVersionInstalled(newVal);
    		    boolean fullLicense = AuthManager.hasFullLicense();

    		    String buttonText;
    		    if (fullLicense) {
    		        buttonText = installed ? I18n.get("menu.play") : I18n.get("menu.installandplay");
    		    } else {
    		        buttonText = installed ? I18n.get("menu.playdemo") : I18n.get("menu.installandplaydemo");
    		    }
    		    playButton.setText(buttonText);
    		    configManager.save();
    	});

    	playButton.setOnAction(e -> {
    	    ConfigManager cfgMgr = new ConfigManager();
    	    LauncherConfig cfg = cfgMgr.getConfig();

    	    UserProfile user = cfg.users.isEmpty() ? null : cfg.users.get(0); // lấy user đầu tiên nếu có
    	    String selectedVersion = cfg.lastVersion != null ? cfg.lastVersion : versionSelect.getValue();

    	    if (selectedVersion == null) {
    	        MessageBox.showError(I18n.get("alert.selectversion"));
    	        return;
    	    }

    	    LoadingGui loading = new LoadingGui(root);
    	    loading.show();

    	    if (user != null) {
    	        // --- User đã lưu, auto-launch ---
    	        UserProfile finalUser = user;
    	        new Thread(() -> {
    	            MinecraftLauncher.Session session;

    	            if (finalUser.accessToken == null || finalUser.accessToken.isEmpty()) {
    	                session = AuthManager.loginOffline(finalUser.username);
    	            } else {
    	                session = AuthManager.loginLocal(finalUser.username, finalUser.accessToken);
    	            }

    	            Platform.runLater(() -> {
    	                boolean installed = MinecraftLauncher.isVersionInstalled(selectedVersion);
    	                boolean full = AuthManager.getCurrentAccount().isFullLicense();

    	                if (MinecraftLauncher.isLegacyVersion(selectedVersion)) {
    	                    playButton.setText(installed
    	                            ? I18n.get("menu.playlegacy")
    	                            : I18n.get("menu.installandplaylegacy"));
    	                } else {
    	                    playButton.setText(installed
    	                            ? (full ? I18n.get("menu.play") : I18n.get("menu.playdemo"))
    	                            : (full ? I18n.get("menu.installandplay") : I18n.get("menu.installandplaydemo")));
    	                }
    	            });

    	            try {
    	                MinecraftLauncher.launch(selectedVersion, session,
    	                    (progress, status) -> Platform.runLater(() -> loading.setProgress(progress, status)));
    	                Platform.runLater(loading::hide);
    	                Platform.runLater(this::showOverlay);
    	            } catch (Exception ex) {
    	                ex.printStackTrace();
    	                Platform.runLater(() -> {
    	                    loading.hide();
    	                    MessageBox.showError(I18n.get("alert.launchfail", selectedVersion, ex.getMessage()));
    	                });
    	            }
    	        }).start();

    	    } else {
    	        // --- Chưa có user, hiển thị LoginAuthBox ---
    	        Platform.runLater(() -> {
    	            LoginAuthBox.show((method, data) -> {
    	                new Thread(() -> {
    	                    MinecraftLauncher.Session session;
    	                    UserProfile newUser;

    	                    switch (method) {
    	                        case "offline" -> {
    	                            session = AuthManager.loginOffline(data);
    	                            newUser = new UserProfile(data, "", "", selectedVersion, 2048);
    	                        }
    	                        case "local" -> {
    	                            String[] parts = data.split(":", 2);
    	                            session = AuthManager.loginLocal(parts[0], parts.length > 1 ? parts[1] : "");
    	                            newUser = new UserProfile(parts[0], "", parts.length > 1 ? parts[1] : "", selectedVersion, 2048);
    	                        }
    	                        case "ten_auth" -> {
    	                            String[] parts = data.split(":", 2);
    	                            session = AuthManager.loginTenAuth(parts[0], parts.length > 1 ? parts[1] : "");
    	                            newUser = new UserProfile(parts[0], "", parts.length > 1 ? parts[1] : "", selectedVersion, 2048);
    	                        }
    	                        case "microsoft" -> {
    	                            session = AuthManager.loginMicrosoft();
    	                            newUser = new UserProfile(session.username(), session.uuid(), session.accessToken(), selectedVersion, 2048);
    	                        }
    	                        default -> {
    	                            session = AuthManager.loginOffline("Player");
    	                            newUser = new UserProfile("Player", "", "", selectedVersion, 2048);
    	                        }
    	                    }

    	                    // Lưu user mới vào config
    	                    cfgMgr.addOrUpdateUser(newUser);
    	                    cfg.lastVersion = selectedVersion;
    	                    cfgMgr.save();

    	                    Platform.runLater(() -> {
    	                        boolean installed = MinecraftLauncher.isVersionInstalled(selectedVersion);
    	                        boolean full = AuthManager.getCurrentAccount().isFullLicense();

    	                        if (MinecraftLauncher.isLegacyVersion(selectedVersion)) {
    	                            playButton.setText(installed
    	                                    ? I18n.get("menu.playlegacy")
    	                                    : I18n.get("menu.installandplaylegacy"));
    	                        } else {
    	                            playButton.setText(installed
    	                                    ? (full ? I18n.get("menu.play") : I18n.get("menu.playdemo"))
    	                                    : (full ? I18n.get("menu.installandplay") : I18n.get("menu.installandplaydemo")));
    	                        }
    	                    });

    	                    try {
    	                        MinecraftLauncher.launch(selectedVersion, session,
    	                            (progress, status) -> Platform.runLater(() -> loading.setProgress(progress, status)));
    	                        Platform.runLater(loading::hide);
    	                        Platform.runLater(this::showOverlay);
    	                    } catch (Exception ex) {
    	                        ex.printStackTrace();
    	                        Platform.runLater(() -> {
    	                            loading.hide();
    	                            MessageBox.showError(I18n.get("alert.launchfail", selectedVersion, ex.getMessage()));
    	                        });
    	                    }
    	                }).start();
    	            });
    	            LoginAuthBox.setOnCancel(() -> loading.hide());
    	        });
    	    }
    	});


    	playBox.getChildren().addAll(playIcon, playButton, versionSelect);
    	homePage.getChildren().add(playBox);

    	// --- Accounts page ---
    	VBox authPage = new VBox(12);
    	authPage.setAlignment(Pos.TOP_CENTER);
    	authPage.setPadding(new Insets(12));

    	Text authText = new Text(I18n.get("menu.accounts"));
    	authText.setFill(Color.WHITE);

    	Button loginBtn = new Button(I18n.get("accounts.loginorsignup"));
    	loginBtn.setOnAction(e -> LoginAuthBox.show(null));

    	authPage.getChildren().addAll(authText, loginBtn);

    	// --- Missions page ---
    	VBox missionsPage = new VBox(12);
    	missionsPage.setAlignment(Pos.TOP_CENTER);
    	missionsPage.setPadding(new Insets(30));
    	missionsPage.setStyle("-fx-background-color: rgba(50,50,50,0.5); -fx-background-radius: 15; -fx-border-radius: 15; -fx-border-color: #00aaff; -fx-border-width: 2;");

    	Text missionsTitle = new Text(I18n.get("menu.missions"));
    	missionsTitle.setStyle("-fx-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");

    	ListView<String> missionsList = new ListView<>();
    	missionsList.getItems().addAll(I18n.get("missions.m1"), I18n.get("missions.m2"), I18n.get("missions.m3"));
    	missionsList.setPrefHeight(200);
    	missionsList.setStyle("-fx-background-color: #2a2a2a; -fx-border-radius: 12; -fx-background-radius: 12; -fx-border-color: #00aaff; -fx-border-width: 2;");

    	missionsPage.getChildren().addAll(missionsTitle, missionsList);
    	
    	VBox artistPage = new VBox(12);
    	artistPage.setAlignment(Pos.TOP_CENTER);
    	artistPage.setPadding(new Insets(20));
    	artistPage.setStyle("-fx-background-color: #1e1e1e; -fx-background-radius: 15; -fx-border-radius: 15; -fx-border-color: #00ffaa; -fx-border-width: 2;");

    	Text artistTitle = new Text("Artists");
    	artistTitle.setStyle("-fx-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");

    	// Danh sách artist (demo)
    	ListView<String> artistList = new ListView<>();
    	artistList.getItems().addAll(
    	    "Alex Johnson",
    	    "Maria Lopez",
    	    "Takashi Nakamura",
    	    "Lily Chen"
    	);
    	artistList.setPrefHeight(200);
    	artistList.setStyle("-fx-background-color: #2a2a2a; -fx-border-radius: 12; -fx-background-radius: 12; -fx-border-color: #00ffaa; -fx-border-width: 2;");

    	artistPage.getChildren().addAll(artistTitle, artistList);

    	VBox modsPage = new VBox(12);
        modsPage.setAlignment(Pos.TOP_CENTER);
        modsPage.setPadding(new Insets(20));
        modsPage.setStyle("-fx-background-color: rgba(50,50,50,0.3); -fx-background-radius: 15; -fx-border-radius: 15; -fx-border-color: #00aaff; -fx-border-width: 2;");

        Text modsTitle = new Text("Mods");
        modsTitle.setStyle("-fx-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");

        HBox topListBar = new HBox(10);
        topListBar.setAlignment(Pos.CENTER_LEFT);

        TextField searchField = new TextField();
        searchField.setPromptText("Search mods...");
        searchField.setPrefWidth(200);

        Button searchBtn = new Button("Search");

        ComboBox<String> categoryBox = new ComboBox<>();
        categoryBox.getItems().addAll("All", "Utility", "Performance", "Tech", "Magic");
        categoryBox.setValue("All");

        topListBar.getChildren().addAll(searchField, searchBtn, new Label("Category"), categoryBox);

        VBox installedBox = new VBox(8);
        installedBox.setPrefWidth(200);

        Text installedTitle = new Text("Installed Mods");
        installedTitle.setStyle("-fx-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

        ListView<String> installedMods = new ListView<>();
        installedMods.getItems().addAll("OptiFine", "BetterFps", "XaerosMinimap");

        installedBox.getChildren().addAll(installedTitle, installedMods);

        VBox modsListContainer = new VBox(15);
        modsListContainer.setPadding(new Insets(10));
        ScrollPane modsScroll = new ScrollPane(modsListContainer);
        modsScroll.setFitToWidth(true);
        modsScroll.setStyle("-fx-background: #2f3136;");

        Function<JSONObject, VBox> createModCard = (JSONObject mod) -> {
            VBox card = new VBox(5);
            card.setPadding(new Insets(10));
            card.setStyle("-fx-background-color: rgba(50,50,50,0,83); -fx-background-radius: 8;");

            // --- Top bar (thumbnail + text) ---
            HBox topBarContainer = new HBox(10);
            topBarContainer.setAlignment(Pos.CENTER_LEFT);
            topBarContainer.setStyle("-fx-background-color: rgba(50,50,50, 0.63)");

            // Thumbnail với bo góc
            String iconUrl = mod.optString("icon_url", "");
            ImageView thumbnail = new ImageView();
            thumbnail.setFitWidth(64);
            thumbnail.setFitHeight(64);
            thumbnail.setPreserveRatio(true);
            if (!iconUrl.isEmpty()) {
                thumbnail.setImage(loadImage(iconUrl));
            }

            Rectangle clip = new Rectangle(thumbnail.getFitWidth(), thumbnail.getFitHeight());
            clip.setArcWidth(16);
            clip.setArcHeight(16);
            thumbnail.setClip(clip);

            // Text: title + description + author
            VBox textBox = new VBox(3);
            Text modTitle = new Text(mod.getString("title"));
            modTitle.setStyle("-fx-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

            Text modDesc = new Text(mod.optString("description", "No description"));
            modDesc.setStyle("-fx-fill: #cccccc; -fx-font-size: 12px;");

            // --- Author info ---
            HBox authorBox = new HBox(5);
            authorBox.setAlignment(Pos.CENTER_LEFT);

            String authorId = mod.optString("author_id", ""); // Lấy author_id
            String authorName = "Unknown";
            String authorAvatarUrl = "";

            if (!authorId.isEmpty()) {
                try {
                    JSONObject userJson = fetchUser(authorId);
                    // Debug log
                    System.out.println("Fetched user info:");
                    System.out.println("ID: " + userJson.optString("id"));
                    System.out.println("Username: " + userJson.optString("username"));
                    System.out.println("Avatar URL: " + userJson.optString("avatar_url"));
                    System.out.println("-------------------------");

                    authorName = userJson.optString("username", "Unknown");
                    authorAvatarUrl = userJson.optString("avatar_url", "");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            ImageView authorAvatar = new ImageView();
            authorAvatar.setFitWidth(24);
            authorAvatar.setFitHeight(24);
            authorAvatar.setPreserveRatio(true);
            if (!authorAvatarUrl.isEmpty()) {
                authorAvatar.setImage(loadImage(authorAvatarUrl));
            }

            Rectangle avatarClip = new Rectangle(authorAvatar.getFitWidth(), authorAvatar.getFitHeight());
            avatarClip.setArcWidth(12);
            avatarClip.setArcHeight(12);
            authorAvatar.setClip(avatarClip);

            Label authorLabel = new Label(authorName);
            authorLabel.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 12px;");
            authorLabel.setOnMouseClicked(e -> {
            	this.showPage(pagesContainer, artistPage);
            });

            authorBox.getChildren().addAll(authorAvatar, authorLabel);
            textBox.getChildren().addAll(modTitle, modDesc, authorBox);

            topBarContainer.getChildren().addAll(thumbnail, textBox);

            // --- Bottom bar (downloads + install button) ---
            HBox bottomBar = new HBox(10);
            bottomBar.setAlignment(Pos.CENTER_LEFT);

            Label downloads = new Label("Downloads: " + mod.optInt("downloads", 0));
            downloads.setStyle("-fx-text-fill: #aaaaaa;");

            Button installBtn = new Button(installedMods.getItems().contains(mod.getString("title")) ? "Delete" : "Install");
            installBtn.setOnAction(e -> {
                String modName = mod.getString("title");

                if (!installedMods.getItems().contains(modName)) {
                    installedMods.getItems().add(modName);
                    installBtn.setText("Delete");

                    // --- Download mod in background ---
                    new Thread(() -> {
                        try {
                            // Lấy projectId và phiên bản mới nhất
                            String projectId = mod.getString("id");
                            JSONArray versions = mod.optJSONArray("versions");
                            if (versions != null && versions.length() > 0) {
                                String versionId = versions.getString(0);
                                downloadMod(projectId, versionId); // Tải về mods/
                                Platform.runLater(() -> {
                                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                    alert.setHeaderText("Mod Installed");
                                    alert.setContentText(modName + " đã được tải về thư mục mods/");
                                    alert.show();
                                });
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            Platform.runLater(() -> {
                                Alert alert = new Alert(Alert.AlertType.ERROR);
                                alert.setHeaderText("Download Failed");
                                alert.setContentText("Không thể tải mod: " + modName);
                                alert.show();
                            });
                        }
                    }).start();

                } else {
                    installedMods.getItems().remove(modName);
                    installBtn.setText("Install");

                    // --- Delete mod file from mods/ ---
                    Path modsDir = Paths.get("mods");
                    try (DirectoryStream<Path> stream = Files.newDirectoryStream(modsDir)) {
                        for (Path file : stream) {
                            if (file.getFileName().toString().toLowerCase().contains(modName.toLowerCase())) {
                                Files.delete(file);
                            }
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            });


            bottomBar.getChildren().addAll(downloads, installBtn);
            
            card.getStyleClass().add("mod-card");
            topBarContainer.getStyleClass().add("mod-card-topbar");
            bottomBar.getStyleClass().add("mod-card-bottombar");
            downloads.getStyleClass().add("mod-card-downloads");
            installBtn.getStyleClass().add("mod-card-button");
            modTitle.getStyleClass().add("mod-card-title");
            modDesc.getStyleClass().add("mod-card-desc");
            authorLabel.getStyleClass().add("mod-card-author");

            // --- Assemble card ---
            card.getChildren().addAll(topBarContainer, bottomBar);
            return card;
        };

        // --- Search action ---
        searchBtn.setOnAction(e -> {
            modsListContainer.getChildren().clear();
            String query = searchField.getText();
            String category = categoryBox.getValue().toLowerCase();

            new Thread(() -> {
                try {
                    JSONArray mods = fetchModsFromModrinth(query);
                    Platform.runLater(() -> {
                        for (int i = 0; i < mods.length(); i++) {
                            JSONObject mod = mods.getJSONObject(i);
                            // Filter by category if not "All"
                            if (!category.equals("all") && !mod.optJSONArray("categories").toList().contains(category)) continue;
                            modsListContainer.getChildren().add(createModCard.apply(mod));
                        }
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }).start();
        });

        HBox contentLayout = new HBox(15);
        contentLayout.getChildren().addAll(installedBox, modsScroll);

        modsPage.getChildren().addAll(modsTitle, topListBar, contentLayout);
    	// --- Settings page ---
     // ===== SETTINGS PAGE =====
        VBox settingsPage = new VBox(10);
        settingsPage.setPadding(new Insets(15));
        settingsPage.setStyle("-fx-background-color: rgba(50,50,50,0.3); -fx-background-radius: 15; -fx-border-radius: 15; -fx-border-color: #00aaff; -fx-border-width: 2;");
        VBox.setVgrow(settingsPage, Priority.ALWAYS);

        // ===== TAB MENU =====
        TabPane settingsTabPane = new TabPane();
        settingsTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        settingsTabPane.setTabMinHeight(25);
        settingsTabPane.setTabMaxHeight(30);
        settingsTabPane.setTabMinWidth(100);
        settingsTabPane.setTabMaxWidth(120);
        settingsTabPane.setStyle("-fx-background-border: 8; -fx-border-radius: 8;");
        VBox.setVgrow(settingsTabPane, Priority.ALWAYS);
        
        settingsTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
        	if (newTab != null && newTab.getContent() != null) {
                Node content = newTab.getContent();
                content.setTranslateX(40); // bắt đầu trượt nhẹ từ phải
                content.setOpacity(0);

                Timeline timeline = new Timeline(
                    new KeyFrame(Duration.ZERO,
                        new KeyValue(content.translateXProperty(), 40),
                        new KeyValue(content.opacityProperty(), 0)
                    ),
                    new KeyFrame(Duration.millis(500),
                        new KeyValue(content.translateXProperty(), 0, CUBIC_OUT),
                        new KeyValue(content.opacityProperty(), 1, CUBIC_OUT)
                    )
                );

                timeline.play();
            }
        });


        // ===== GENERAL TAB =====
        GridPane generalSettingsGrid = new GridPane();
        generalSettingsGrid.setHgap(20);
        generalSettingsGrid.setVgap(10);
        generalSettingsGrid.setPadding(new Insets(10));
        generalSettingsGrid.setStyle("-fx-background-border: 8; -fx-border-radius: 8;");

        int rowGeneral = 0;

        // Game Path
        generalSettingsGrid.add(new Label(I18n.get("settings.gamepath")), 0, rowGeneral);
        TextField gamePathField = new TextField(defaultGamePath);
        gamePathField.setPrefWidth(350);
        generalSettingsGrid.add(gamePathField, 1, rowGeneral++);
        Button browseBtn = new Button(I18n.get("settings.browse"));
        generalSettingsGrid.add(browseBtn, 2, rowGeneral - 1);

        browseBtn.setOnAction(e -> {
            OpenFileBox.show(I18n.get("settings.selectfolder"), new File(System.getProperty("user.home")), selected -> {
                if (selected != null) {
                    selectedGamePath = selected.getAbsolutePath();
                    gamePathField.setText(selectedGamePath);
                    versionSelect.getItems().clear();
                    versionSelect.getItems().addAll(loadMinecraftVersions(selectedGamePath));
                }
            });
        });

        // Auto Update
        generalSettingsGrid.add(new Label(I18n.get("settings.autoupdate")), 0, rowGeneral);
        CheckBox autoUpdate = new CheckBox();
        autoUpdate.setSelected(cfg.autoUpdate);
        generalSettingsGrid.add(autoUpdate, 1, rowGeneral++);

        // Theme
        generalSettingsGrid.add(new Label(I18n.get("settings.theme")), 0, rowGeneral);
        ComboBox<String> themeSelect = new ComboBox<>();
        themeSelect.getItems().addAll(
            I18n.get("settings.theme.light"),
            I18n.get("settings.theme.dark"),
            I18n.get("settings.theme.system")
        );

        themeSelect.setValue(cfg.theme);
        
        this.apply(themeSelect);
        
        generalSettingsGrid.add(themeSelect, 1, rowGeneral++);

        // Buttons
        HBox saveBox = new HBox(10);
        saveBox.setAlignment(Pos.CENTER_RIGHT);
        Button saveBtn = new Button(I18n.get("settings.save"));
        saveBox.getChildren().add(saveBtn);

        saveBtn.setOnAction(e -> {
            cfg.gamePath = gamePathField.getText();
            cfg.autoUpdate = autoUpdate.isSelected();
            cfg.theme = themeSelect.getValue();

            configManager.save();
            this.showNotification(root, I18n.get("settings.saved"), 2000);

            versionSelect.getItems().clear();
            versionSelect.getItems().addAll(loadMinecraftVersions(cfg.gamePath));
        });

        // General tab content
        VBox generalSettingsContent = new VBox(10, generalSettingsGrid, saveBox);
        generalSettingsContent.setPadding(new Insets(10));
        generalSettingsContent.setStyle("-fx-background-color: rgba(50,50,50,0.3); -fx-background-radius: 15; -fx-border-radius: 15;");

        ScrollPane generalSettingsScroll = new ScrollPane(generalSettingsContent);
        generalSettingsScroll.setFitToWidth(true);
        generalSettingsScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        Tab generalSettingsTab = new Tab(I18n.get("settings.general"), generalSettingsScroll);
        generalSettingsTab.setClosable(false);

        // ===== ADVANCED TAB =====
        GridPane advSettingsGrid = new GridPane();
        advSettingsGrid.setHgap(20);
        advSettingsGrid.setVgap(10);
        advSettingsGrid.setPadding(new Insets(10));

        int rowAdv = 0;

        advSettingsGrid.add(new Label("Enable Logging"), 0, rowAdv);
        CheckBox loggingBox = new CheckBox();
        loggingBox.setSelected(true);
        advSettingsGrid.add(loggingBox, 1, rowAdv++);

        advSettingsGrid.add(new Label("Experimental Features"), 0, rowAdv);
        CheckBox expFeatures = new CheckBox();
        advSettingsGrid.add(expFeatures, 1, rowAdv++);

        VBox advSettingsContent = new VBox(10, advSettingsGrid);
        advSettingsContent.setPadding(new Insets(10));
        advSettingsContent.setStyle("-fx-background-color: rgba(50,50,50,0.4); -fx-background-radius: 15; -fx-border-radius: 15;");

        ScrollPane advSettingsScroll = new ScrollPane(advSettingsContent);
        advSettingsScroll.setFitToWidth(true);
        advSettingsScroll.setStyle("-fx-background: transparent; -fx-background-radius: 8; -fx-border-radius: 8;");

        Tab advSettingsTab = new Tab(I18n.get("settings.advanced"), advSettingsScroll);
        advSettingsTab.setClosable(false);

        // ===== Add Tabs =====
        settingsTabPane.getTabs().addAll(generalSettingsTab, advSettingsTab);
        settingsPage.getChildren().add(settingsTabPane);

    	
    	// --- Artist Page ---

    	// --- Skins page ---
    	VBox skinPage = new VBox(10);
    	skinPage.setPadding(new Insets(15));
    	skinPage.setStyle("-fx-background-color: rgba(50,50,50,0.4)"); // nền nâu Minecraft

    	// ===== TAB MENU =====
    	TabPane tabPane = new TabPane();
    	tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

    	// chỉnh kích cỡ tabPane
    	tabPane.setTabMinHeight(25);  // chiều cao tối thiểu
    	tabPane.setTabMaxHeight(30);  // chiều cao tối đa
    	tabPane.setTabMinWidth(90);   // chiều rộng tối thiểu mỗi tab
    	tabPane.setTabMaxWidth(90);

    	VBox.setVgrow(tabPane, Priority.ALWAYS);


    	// ===== GRID OPTIONS =====
    	GridPane optionsGrid = new GridPane();
    	optionsGrid.setHgap(20);
    	optionsGrid.setVgap(10);
    	optionsGrid.setPadding(new Insets(10));

    	int row = 0;

    	// Render Distance
    	optionsGrid.add(new Label("Render Distance"), 0, row);
    	ComboBox<String> renderDistance = new ComboBox<>();
    	renderDistance.getItems().addAll("8 chunks", "12 chunks", "16 chunks");
    	renderDistance.setValue("12 chunks");
    	optionsGrid.add(renderDistance, 1, row++);

    	// Max Shadow Distance
    	optionsGrid.add(new Label("Max Shadow Distance"), 0, row);
    	ComboBox<String> shadowDistance = new ComboBox<>();
    	shadowDistance.getItems().addAll("16 Chunks", "32 Chunks", "64 Chunks");
    	shadowDistance.setValue("32 Chunks");
    	optionsGrid.add(shadowDistance, 1, row++);

    	// Simulation Distance
    	optionsGrid.add(new Label("Simulation Distance"), 0, row);
    	ComboBox<String> simDistance = new ComboBox<>();
    	simDistance.getItems().addAll("8 chunks", "12 chunks", "20 chunks");
    	simDistance.setValue("12 chunks");
    	optionsGrid.add(simDistance, 1, row++);

    	// Brightness
    	optionsGrid.add(new Label("Brightness"), 0, row);
    	ComboBox<String> brightness = new ComboBox<>();
    	brightness.getItems().addAll("Moody", "Bright");
    	brightness.setValue("Moody");
    	optionsGrid.add(brightness, 1, row++);

    	// GUI Scale
    	optionsGrid.add(new Label("GUI Scale"), 0, row);
    	ComboBox<String> guiScale = new ComboBox<>();
    	guiScale.getItems().addAll("Small", "Normal", "Large", "Auto");
    	guiScale.setValue("Auto");
    	optionsGrid.add(guiScale, 1, row++);

    	// Fullscreen
    	optionsGrid.add(new Label("Fullscreen"), 0, row);
    	CheckBox fullscreen = new CheckBox();
    	optionsGrid.add(fullscreen, 1, row++);

    	// VSync
    	optionsGrid.add(new Label("VSync"), 0, row);
    	CheckBox vsync = new CheckBox();
    	vsync.setSelected(true);
    	optionsGrid.add(vsync, 1, row++);

    	// Max Framerate
    	optionsGrid.add(new Label("Max Framerate"), 0, row);
    	ComboBox<String> framerate = new ComboBox<>();
    	framerate.getItems().addAll("30", "60", "120", "Unlimited");
    	framerate.setValue("Unlimited");
    	optionsGrid.add(framerate, 1, row++);

    	// View Bobbing
    	optionsGrid.add(new Label("View Bobbing"), 0, row);
    	CheckBox bobbing = new CheckBox();
    	bobbing.setSelected(true);
    	optionsGrid.add(bobbing, 1, row++);

    	// Attack Indicator
    	optionsGrid.add(new Label("Attack Indicator"), 0, row);
    	ComboBox<String> attackInd = new ComboBox<>();
    	attackInd.getItems().addAll("Crosshair", "Hotbar", "Off");
    	attackInd.setValue("Crosshair");
    	optionsGrid.add(attackInd, 1, row++);

    	// Autosave Indicator
    	optionsGrid.add(new Label("Autosave Indicator"), 0, row);
    	CheckBox autosave = new CheckBox();
    	autosave.setSelected(true);
    	optionsGrid.add(autosave, 1, row++);

    	// ===== BUTTONS =====
    	HBox buttons = new HBox(10);
    	buttons.setAlignment(Pos.CENTER_RIGHT);
    	Button applyBtn = new Button("Apply");
    	Button doneBtn = new Button("Done");
    	buttons.getChildren().addAll(applyBtn, doneBtn);

    	// ===== BUILD =====
    	Tab generalTab = new Tab("General", new VBox(optionsGrid, buttons));
    	generalTab.setClosable(false);
    	
    	// ===== SKIN TAB FULL =====

    	// Container preview skins
    	FlowPane skinContainer = new FlowPane();
    	skinContainer.setPadding(new Insets(15));
    	skinContainer.setHgap(20);
    	skinContainer.setVgap(20);
    	skinContainer.setPrefWrapLength(500);
    	skinContainer.setAlignment(Pos.TOP_LEFT);
    	skinContainer.setStyle("-fx-background-color: rgba(0,0,0,0.5)");

    	// ScrollPane
    	ScrollPane scrollPane = new ScrollPane(skinContainer);
    	scrollPane.setFitToWidth(true);
    	scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    	scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
    	scrollPane.setStyle("-fx-background-color: transparent;");

    	// Label tiêu đề
    	Label skinTitle = new Label("Your Skins");
    	skinTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white; -fx-effect: dropshadow(one-pass-box, rgba(0,0,0,0.6), 2, 0, 1, 1);");

    	// Nút Upload + Inject
    	Button uploadBtn = new Button("Upload Skin");
    	Button injectBtn = new Button("Inject Skin");
    	HBox skinButtons = new HBox(10, uploadBtn, injectBtn);
    	skinButtons.setAlignment(Pos.CENTER_RIGHT);
    	skinButtons.setPadding(new Insets(5, 10, 10, 10));

    	// VBox tổng layout tab
    	VBox skinTabContent = new VBox(10, skinTitle, scrollPane, skinButtons);
    	skinTabContent.setPadding(new Insets(10));
    	skinTabContent.setStyle("-fx-background-color: rgba(50,50,50,0.4);");

    	// ===== Load Skins 2D =====
    	File skinsDir = new File(System.getProperty("user.home"), "AppData/Roaming/.luna/skins");
    	File[] skinFiles = skinsDir.listFiles(f -> f.getName().endsWith(".png"));

    	if (skinFiles != null) {
    	    for (File skinFile : skinFiles) {
    	        try {
    	            Image skin = new Image(skinFile.toURI().toString());
    	            ImageView front = buildMinecraftChar2DFront(skin);
    	            ImageView back = buildMinecraftChar2DBack(skin);

    	            front.setFitWidth(64 * 2);
    	            front.setFitHeight(128 * 2);
    	            front.setSmooth(false);
    	            back.setFitWidth(64 * 2);
    	            back.setFitHeight(128 * 2);
    	            back.setSmooth(false);

    	            HBox previews = new HBox(10, front, back);
    	            previews.setAlignment(Pos.CENTER);

    	            VBox skinBox = new VBox(5, previews, new Label(skinFile.getName()));
    	            skinBox.setAlignment(Pos.CENTER);
    	            skinBox.setStyle(
    	                "-fx-background-color: rgba(255,255,255,0.05);" +
    	                "-fx-background-radius: 10;" +
    	                "-fx-padding: 10;" +
    	                "-fx-effect: dropshadow(one-pass-box, rgba(0,0,0,0.5), 4, 0, 0, 2);"
    	            );

    	            // Hover effect
    	            skinBox.setOnMouseEntered(e -> skinBox.setScaleX(1.05));
    	            skinBox.setOnMouseEntered(e -> skinBox.setScaleY(1.05));
    	            skinBox.setOnMouseExited(e -> skinBox.setScaleX(1.0));
    	            skinBox.setOnMouseExited(e -> skinBox.setScaleY(1.0));

    	            skinContainer.getChildren().add(skinBox);
    	        } catch (Exception ex) {
    	            ex.printStackTrace();
    	        }
    	    }
    	}
    	
    	FileChooser fileChooser = new FileChooser();

    	// ===== Select Skin Logic =====
    	final Object[] selectedSkin = {null};
    	skinContainer.getChildren().forEach(node -> {
    	    node.setOnMouseClicked(ev -> {
    	        skinContainer.getChildren().forEach(n -> n.setStyle(n.getStyle().replace("-fx-border-color: yellow;", "")));
    	        node.setStyle(node.getStyle() + "-fx-border-color: yellow; -fx-border-width: 2;");
    	        selectedSkin[0] = node;
    	    });
    	});

    	injectBtn.setOnAction(ev -> {
    	    try {
    	        fileChooser.setTitle("Select a Minecraft Skin");
    	        fileChooser.getExtensionFilters().add(
    	                new FileChooser.ExtensionFilter("PNG Images", "*.png")
    	        );

    	        // Sử dụng tên biến khác
    	        File skinFileDialog = fileChooser.showOpenDialog(injectBtn.getScene().getWindow());
    	        if (skinFileDialog == null || !skinFileDialog.exists()) {
    	            //showAlert("No skin selected", "Please select a skin to inject.");
    	            return;
    	        }

    	        // Lưu vào biến global Object[]
    	        selectedSkin[0] = skinFileDialog;

    	        // Copy vào .luna/skins/
    	        if (!skinsDir.exists()) skinsDir.mkdirs();

    	        File targetSkin = new File(skinsDir, skinFileDialog.getName());
    	        Files.copy(skinFileDialog.toPath(), targetSkin.toPath(), StandardCopyOption.REPLACE_EXISTING);

    	        // Tạo command line offline
    	        List<String> commandLine = new ArrayList<>();
    	        commandLine.add("--username"); commandLine.add("OfflinePlayer");
    	        commandLine.add("--version");  commandLine.add("1.21");
    	        commandLine.add("--gameDir");  commandLine.add(Main.defaultGamePath);
    	        commandLine.add("--assetsDir");commandLine.add(new File(Main.defaultGamePath, "assets").getAbsolutePath());
    	        commandLine.add("--offlineSkin"); commandLine.add(targetSkin.getAbsolutePath());

    	        // Lưu vào config
    	        LauncherConfig config = new LauncherConfig();
    	        config.lastCommandLine = commandLine;
    	        config.save();

    	        //showAlert("Success", "Skin injected successfully!");
    	    } catch (Exception ex) {
    	        ex.printStackTrace();
    	        //howAlert("Error", "Failed to inject skin: " + ex.getMessage());
    	    }
    	});

    	// ===== Upload Skin Button =====
    	uploadBtn.setOnAction(e -> {
    	    fileChooser.setTitle("Select Skin PNG");
    	    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Images", "*.png"));
    	    File selectedFile = fileChooser.showOpenDialog(skinPage.getScene().getWindow());
    	    if (selectedFile != null) {
    	        try {
    	            if (!skinsDir.exists()) skinsDir.mkdirs();
    	            File dest = new File(skinsDir, selectedFile.getName());
    	            java.nio.file.Files.copy(selectedFile.toPath(), dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);

    	            // reload skins
    	            skinContainer.getChildren().clear();
    	            loadSkins2D(skinContainer); // hàm bạn đã viết
    	        } catch (Exception ex) {
    	            ex.printStackTrace();
    	        }
    	    }
    	});

    	// ===== Tạo Tab Skin =====
    	Tab skinTab = new Tab("Skin", skinTabContent);
    	skinTab.setClosable(false);


    	String[] otherTabs = {"Performance", "Advanced", "Shader Packs..."};
    	for (String t : otherTabs) {
    	    tabPane.getTabs().add(new Tab(t, new Label("Coming soon...")));
    	}

    	tabPane.getTabs().add(0, generalTab); // gắn tab General vào đầu tiên
    	tabPane.getTabs().add(1, skinTab);
    	skinPage.getChildren().add(tabPane);



    	// --- Add pages to container ---
    	pagesContainer.getChildren().addAll(homePage, authPage, missionsPage, modsPage, settingsPage, artistPage, skinPage);
    	showPage(pagesContainer, homePage);

    	layout.setCenter(pagesContainer);

    	// === BOTTOM: Navigation bar ===
    	HBox navBar = new HBox(20);
    	navBar.setAlignment(Pos.CENTER);
    	navBar.setPadding(new Insets(12));
    	navBar.getStyleClass().add("nav-bar");

    	Button home = new Button(I18n.get("menu.home"));
    	home.getStyleClass().add("menu-button");
    	home.setOnAction(e -> showPage(pagesContainer, homePage));

    	Button accounts = new Button(I18n.get("menu.accounts"));
    	accounts.getStyleClass().add("menu-buttons");
    	accounts.setOnAction(e -> showPage(pagesContainer, authPage));

    	Button missions = new Button(I18n.get("menu.missions"));
    	missions.getStyleClass().add("menu-button");
    	missions.setOnAction(e -> showPage(pagesContainer, missionsPage));

    	Button mods = new Button(I18n.get("menu.mods"));
    	mods.getStyleClass().add("menu-button");
    	mods.setOnAction(e -> showPage(pagesContainer, modsPage));
    	
    	Button skins = new Button("Skin");
    	skins.getStyleClass().add("menu-button");
    	skins.setOnAction(e -> showPage(pagesContainer, skinPage));

    	Button settings = new Button(I18n.get("menu.settings"));
    	settings.getStyleClass().add("menu-button");
    	settings.setOnAction(e -> showPage(pagesContainer, settingsPage));

    	navBar.getChildren().addAll(home, accounts, missions, mods, skins, settings);
    	layout.setBottom(navBar);

    	// === Background Image ===
    	// === Auto region background fetch ===
    	String region = null;

    	// thử lấy từ system property
    	try {
    	    region = System.getProperty("user.region");
    	} catch (Exception ignored) {}

    	// nếu null hoặc rỗng, fallback sang locale
    	if (region == null || region.isBlank()) {
    	    try {
    	        region = Locale.getDefault().getCountry();
    	    } catch (Exception ignored) {}
    	}

    	// fallback cuối cùng là VN
    	if (region == null || region.isBlank()) {
    	    region = "vn";
    	}

    	region = region.toLowerCase();

    	String jsonUrl;

    	// chọn URL tương ứng
    	switch (region) {
    	    case "jp":
    	        jsonUrl = "/backgrounds_jp.json";
    	        break;
    	    case "ru":
    	        jsonUrl = "/backgrounds_ru.json";
    	        break;
    	    case "us":
    	        jsonUrl = "/backgrounds_us.json";
    	        break;
    	    case "kr":
    	        jsonUrl = "/backgrounds_kr.json";
    	        break;
    	    default:
    	        jsonUrl = "/backgrounds.json"; // mặc định (VN hoặc global)
    	        break;
    	}

    	logInstance.info("[LunaLauncher] region=" + region);

    	BackgroundImage backgroundImage = null;

    	try {
    	    JSONObject json = fetchJsonFromApi(jsonUrl); // dùng hàm fetchJson ở trên
    	    String bgUrl = json.optString("background", "");

    	    if (!bgUrl.isEmpty()) {
    	        Image img = new Image(bgUrl, true);

    	        backgroundImage = new BackgroundImage(
    	            img,
    	            BackgroundRepeat.NO_REPEAT,
    	            BackgroundRepeat.NO_REPEAT,
    	            BackgroundPosition.CENTER,
    	            new BackgroundSize(
    	                BackgroundSize.AUTO, BackgroundSize.AUTO,
    	                false, false, true, true
    	            )
    	        );

    	        layout.setStyle(
    	            "-fx-background-image: url('" + bgUrl + "');" +
    	            "-fx-background-repeat: no-repeat;" +
    	            "-fx-background-position: center;" +
    	            "-fx-background-size: cover;" +
    	            "-fx-background-radius: 20;" +
    	            "-fx-border-radius: 20;" +
    	            "-fx-background-insets: 0;" +
    	            "-fx-background-clip: padding;"
    	        );
    	    }
    	} catch (Exception e) {
    	    e.printStackTrace();
    	}


        root.setPadding(new Insets(8));
        root.setStyle("-fx-background-color: rgba(30,41,59,0.92); -fx-background-radius: 16; -fx-border-radius: 16; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 20, 0, 0, 4);");

        Scene scene = new Scene(root, 1280, 720);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(getClass().getResource("/application/application.css").toExternalForm());

        // ==== Splash stage ====
     // ==== Splash Stage setup ==== 
        Stage splashStage = new Stage(StageStyle.TRANSPARENT);
        StackPane splashRoot = new StackPane();

        // Background image
        ImageView splashImg = new ImageView(new Image(getClass().getResource("/resources/loadbg.jpg").toExternalForm()));
        splashImg.setFitWidth(400);
        splashImg.setFitHeight(280);
        splashImg.setPreserveRatio(true);

        // Status label
        Label statusLabel = new Label("Starting...");
        statusLabel.setTextFill(Color.CYAN);
        statusLabel.setStyle("-fx-font-family: 'Comic Relief'; -fx-font-size: 13px;");

        // Percent label
        Label percentLabel = new Label("0%");
        percentLabel.setTextFill(Color.CYAN);
        percentLabel.setStyle("-fx-font-family: 'Comic Relief'; -fx-font-size: 13px;");

        // HBox chứa status + %
        HBox statusBox = new HBox(10, statusLabel, percentLabel);
        statusBox.setAlignment(Pos.CENTER);

        // Progress bounce bar
        StackPane progressContainer = new StackPane();
        progressContainer.setPrefSize(300, 10);
        progressContainer.setStyle("-fx-background-color: #444;");
        Rectangle bar = new Rectangle(60, 10, Color.CYAN);
        progressContainer.getChildren().add(bar);
        StackPane.setAlignment(progressContainer, Pos.BOTTOM_CENTER);
        StackPane.setMargin(progressContainer, new Insets(0, 0, 10, 0));

        // Bounce animation
        Timeline bounce = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(bar.translateXProperty(), -180)),
                new KeyFrame(Duration.seconds(5), new KeyValue(bar.translateXProperty(), 180))
        );
        bounce.setCycleCount(Animation.INDEFINITE);
        bounce.setAutoReverse(true);
        bounce.play();

        // VBox tổng
        VBox splashBox = new VBox(20, splashImg, statusBox, progressContainer);
        splashBox.setAlignment(Pos.CENTER);
        splashRoot.getChildren().add(splashBox);

        Scene splashScene = new Scene(splashRoot, 400, 290, Color.TRANSPARENT);
        splashStage.setScene(splashScene);
        splashStage.show();

        // Căn giữa màn hình
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        splashStage.setX((screenBounds.getWidth() - splashStage.getWidth()) / 2);
        splashStage.setY((screenBounds.getHeight() - splashStage.getHeight()) / 2);


        // ==== Danh sách tiến trình động ==== 
        List<Runnable> steps = Arrays.asList(
        	    // B1: Kiểm tra rootDir
        	    () -> {
        	        Path rootDir = Paths.get(this.lunaDir);
        	        if (!Files.exists(rootDir)) {
        	            try {
        	                Files.createDirectories(rootDir);
        	                System.out.println("Created root directory: " + rootDir);
        	            } catch (IOException e) {
        	                e.printStackTrace();
        	            }
        	        }

        	        // Kiểm tra config.json
        	        Path config = rootDir.resolve("config.json");
        	        if (!Files.exists(config)) {
        	            try {
        	                String defaultConfig = "{ \"lang\": \"en\", \"theme\": \"dark\" }";
        	                Files.write(config, defaultConfig.getBytes(StandardCharsets.UTF_8));
        	                System.out.println("Created default config.json");
        	            } catch (IOException e) {
        	                e.printStackTrace();
        	            }
        	        }
        	    },

        	    // B2: Giả lập fetch JSON metadata
        	    () -> {
        	        try {
        	            logInstance.info("Fetching metadata...");
        	            Thread.sleep(1000); // mô phỏng
        	        } catch (InterruptedException e) {
        	            e.printStackTrace();
        	        }
        	    },

        	    // B3: Load config
        	    () -> {
        	        try {
        	            logInstance.info("Loading configuration...");
        	            Thread.sleep(800);
        	        } catch (InterruptedException e) {
        	            e.printStackTrace();
        	        }
        	    }
        	);

        	// ==== Task chạy lần lượt ====
        	Task<Void> loadTask = new Task<>() {
        	    @Override
        	    protected Void call() throws Exception {
        	        int totalSteps = steps.size();
        	        for (int i = 0; i < totalSteps; i++) {
        	            String stepName;
        	            switch (i) {
        	                case 0 -> stepName = "Checking root directory...";
        	                case 1 -> stepName = "Fetching JSON metadata...";
        	                case 2 -> stepName = "Loading configuration...";
        	                default -> stepName = "Processing...";
        	            }
        	            updateMessage(stepName);

        	            // Chạy code của bước đó
        	            steps.get(i).run();

        	            updateProgress(i + 1, totalSteps);
        	        }
        	        return null;
        	    }
        	};

        	// Bind UI
        	statusLabel.textProperty().bind(loadTask.messageProperty());
        	loadTask.progressProperty().addListener((obs, oldVal, newVal) -> {
        	    int percent = (int)(newVal.doubleValue() * 100);
        	    percentLabel.setText(percent + "%");
        	});

        	// Chạy task trong thread nền
        	new Thread(loadTask).start();


        // Bind UI với Task
        statusLabel.textProperty().bind(loadTask.messageProperty());
        loadTask.progressProperty().addListener((obs, oldVal, newVal) -> {
            int percent = (int)(newVal.doubleValue() * 100);
            percentLabel.setText(percent + "%");
        });

        // Khi task hoàn tất
        loadTask.setOnSucceeded(e -> {
            FadeTransition fadeOut = new FadeTransition(Duration.seconds(1), splashRoot);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(ev -> {
                splashStage.close();

                // ==== Main Stage ====
                stage.setScene(scene);
                stage.initStyle(StageStyle.TRANSPARENT);
                stage.setTitle(I18n.get("app.title"));
                stage.show();

                root.setCache(true);

                FadeTransition fadeIn = new FadeTransition(Duration.millis(600), root);
                fadeIn.setFromValue(0);
                fadeIn.setToValue(1);
                fadeIn.setInterpolator(Interpolator.EASE_BOTH);
                fadeIn.play();

                this.showNotification(root, I18n.get("nof.expiredlicense"), 3000);
            });
            fadeOut.play();
        });

        // Chạy task trong thread nền
        new Thread(loadTask).start();



        // ==== Animation buttons & notification ====
        addButtonAnimation(playButton);
        addButtonAnimation(accounts);
        addButtonAnimation(home);
        addButtonAnimation(missions);
        addButtonAnimation(mods);
        addButtonAnimation(skins);
        addButtonAnimation(settings);
        addButtonAnimation(minimize);
        addButtonAnimation(close);
        //addButtonAnimation(addSkinBtn);
    }
    
    private void showNotification(StackPane parent, String message, int durationMillis) {
        Popup popup = new Popup();

        Label label = new Label(message);
        label.setStyle(
            "-fx-background-color: #333333;" +
            "-fx-text-fill: white;" +
            "-fx-padding: 12px 20px;" +
            "-fx-background-radius: 12;" +
            "-fx-border-radius: 12;" +
            "-fx-border-color: #ff5555;" +
            "-fx-border-width: 2;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 6, 0, 0, 2);" +
            "-fx-font-size: 14px;"
        );
        label.setFont(new Font(14));

        popup.getContent().add(label);

        // Tính yOffset dựa trên popup hiện tại
        double yOffset = baseY;
        for (Popup p : activePopups) {
            yOffset -= (p.getHeight() + spacing - 8); // trừ thêm 8px
        }

        popup.show(parent, baseX, yOffset);
        activePopups.add(popup);

        // Slide-in + fade-in
        label.setTranslateY(50);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), label);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        TranslateTransition slideIn = new TranslateTransition(Duration.millis(300), label);
        slideIn.setFromY(50);
        slideIn.setToY(0);
        slideIn.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition appear = new ParallelTransition(fadeIn, slideIn);
        appear.play();

        // Khi click vào popup, đóng ngay
        label.setOnMouseClicked(e -> hidePopup(popup));

        // Ẩn sau durationMillis với slide-out + fade-out
        PauseTransition wait = new PauseTransition(Duration.millis(durationMillis));
        wait.setOnFinished(e -> hidePopup(popup));
        wait.play();

        // Khi popup mới xuất hiện, trượt các popup cũ lên
        adjustPopups();
    }

    private void hidePopup(Popup popup) {
        if (!activePopups.contains(popup)) return;

        Label label = (Label) popup.getContent().get(0);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), label);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);

        TranslateTransition slideOut = new TranslateTransition(Duration.millis(300), label);
        slideOut.setFromY(0);
        slideOut.setToY(50);
        slideOut.setInterpolator(Interpolator.EASE_IN);

        ParallelTransition disappear = new ParallelTransition(fadeOut, slideOut);
        disappear.setOnFinished(ev -> {
            popup.hide();
            activePopups.remove(popup);
            adjustPopups();
        });
        disappear.play();
    }

    private void adjustPopups() {
        double yOffset = baseY;
        for (Popup p : activePopups) {
            Label label = (Label) p.getContent().get(0);
            label.applyCss();
            label.layout(); // cập nhật kích thước chính xác

            // Animation dịch chuyển label mượt
            TranslateTransition move = new TranslateTransition(Duration.millis(200), label);
            move.setToY(0);
            move.setInterpolator(Interpolator.EASE_BOTH);
            move.play();

            // Cập nhật vị trí popup theo chiều rộng label
            double popupWidth = label.getWidth();
            p.setX(baseX - popupWidth + 325);  // chữ cuối sát góc phải (cộng thêm 325)
            p.setY(yOffset - label.getHeight());

            yOffset -= (label.getHeight() + spacing); // offset chuẩn cho popup tiếp theo
            //TODO: đã chỉnh BaseY lên 660
        }
    }



    private void showPage(StackPane container, Node page) {
        if (currentPage == page) return;

        Node old = currentPage;
        if (old != null) {
            FadeTransition out = new FadeTransition(Duration.millis(160), old);
            out.setFromValue(1.0);
            out.setToValue(0.0);
            out.setOnFinished(e -> {
                old.setVisible(false);
                page.setOpacity(0.0);
                page.setVisible(true);
                FadeTransition in = new FadeTransition(Duration.millis(200), page);
                in.setFromValue(0.0);
                in.setToValue(1.0);
                in.play();
                currentPage = page;
            });
            out.play();
        } else {
            page.setOpacity(0.0);
            page.setVisible(true);
            FadeTransition in = new FadeTransition(Duration.millis(220), page);
            in.setFromValue(0.0);
            in.setToValue(1.0);
            in.play();
            currentPage = page;
        }

        for (Node n : container.getChildren()) {
            if (n != page && n != old) {
                n.setVisible(false);
                n.setOpacity(0.0);
            }
        }
    }

    /*private void addButtonAnimation(Button button) {
        // Lưu style gốc
        final String baseStyle = button.getStyle();

        // --- Hover effect ---
        button.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), button);
            st.setToX(1.08);
            st.setToY(1.08);
            st.play();

            // Hover shadow + gradient
            button.setStyle(
                    baseStyle +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 8, 0, 0, 4);" +
                    "-fx-background-color: linear-gradient(to bottom, #4facfe, #00f2fe);"
            );
        });

        button.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), button);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();

            // Reset về style gốc
            button.setStyle(baseStyle);
        });

        // --- Press effect ---
        button.setOnMousePressed(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(120), button);
            st.setToX(0.95);
            st.setToY(0.95);
            st.play();

            FadeTransition ft = new FadeTransition(Duration.millis(120), button);
            ft.setFromValue(1.0);
            ft.setToValue(0.8);
            ft.setAutoReverse(true);
            ft.setCycleCount(2);
            ft.play();
        });

        button.setOnMouseReleased(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(120), button);
            st.setToX(1.08);
            st.setToY(1.08);
            st.play();

            FadeTransition ft = new FadeTransition(Duration.millis(120), button);
            ft.setFromValue(0.8);
            ft.setToValue(1.0);
            ft.play();

            // Reset về hover nếu chuột vẫn ở trên button, hoặc baseStyle nếu chuột rời
            if (!button.isHover()) {
                button.setStyle(baseStyle);
            } else {
                button.setStyle(
                    baseStyle +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 8, 0, 0, 4);" +
                    "-fx-background-color: linear-gradient(to bottom, #4facfe, #00f2fe);"
                );
            }
        });
    }*/
    private void addButtonAnimation(Button button) {
        final String baseStyle = button.getStyle();

        button.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), button);
            st.setToX(1.08);
            st.setToY(1.08);
            st.play();

            // Animation mượt màu nền
            Timeline hoverAnim = new Timeline(
                new KeyFrame(Duration.ZERO,
                    new KeyValue(button.backgroundProperty(), button.getBackground())
                ),
                new KeyFrame(Duration.millis(200),
                    new KeyValue(button.styleProperty(),
                        baseStyle + "-fx-background-color: linear-gradient(to bottom, #4facfe, #00f2fe);"
                    )
                )
            );
            hoverAnim.play();
            button.setEffect(new javafx.scene.effect.DropShadow(8, Color.rgb(0,0,0,0.25)));
        });

        button.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), button);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();

            Timeline exitAnim = new Timeline(
                new KeyFrame(Duration.ZERO,
                    new KeyValue(button.styleProperty(), button.getStyle())
                ),
                new KeyFrame(Duration.millis(200),
                    new KeyValue(button.styleProperty(), baseStyle)
                )
            );
            exitAnim.play();
            button.setEffect(null);
        });

        button.setOnMousePressed(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(120), button);
            st.setToX(0.95);
            st.setToY(0.95);
            st.play();
        });

        button.setOnMouseReleased(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(120), button);
            st.setToX(1.08);
            st.setToY(1.08);
            st.play();
        });
    }



    private List<String> loadMinecraftVersions(String minecraftDir) {
        List<String> versions = new ArrayList<>();
        File versionsDir = new File(minecraftDir, "versions");
        if (versionsDir.exists() && versionsDir.isDirectory()) {
            for (File f : versionsDir.listFiles()) {
                if (f.isDirectory() && new File(f, f.getName() + ".json").exists()) {
                    versions.add(f.getName());
                }
            }
        }
        return versions;
    }

    public static void main(String[] theNextWashingMachine) throws Exception {
    	logInstance.info("Starting....");
    	OptionParser parser = new OptionParser();

        parser.accepts("debug").withOptionalArg().ofType(Boolean.class);
        parser.accepts("verbose").withOptionalArg().ofType(Boolean.class);
        parser.accepts("nosplash").withOptionalArg().ofType(Boolean.class);

        OptionSet options = parser.parse(theNextWashingMachine);

        debugMode = options.has("debug");
        verbose = options.has("verbose");
        noSplash = options.has("nosplash");

        if (debugMode) logInstance.info("[DEBUG] Debug mode enabled");
        if (verbose) logInstance.info("[VERBOSE] Verbose logging enabled");
        if (noSplash) logInstance.info("[INFO] Splash screen disabled");

        launch(theNextWashingMachine); // JavaFX start()
    }

	private void hide() {
	}
}
