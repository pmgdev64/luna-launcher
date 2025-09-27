package application;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import vn.pmgteam.kclient.ConfigManager;
import vn.pmgteam.kclient.I18n;
import vn.pmgteam.kclient.LauncherConfig;
import vn.pmgteam.kclient.LoadingGui;
import vn.pmgteam.kclient.MessageBox;
import vn.pmgteam.kclient.OpenFileBox;
import vn.pmgteam.kclient.UserProfile;
import vn.pmgteam.kclient.MinecraftLauncher;
import vn.pmgteam.kclient.NofiticationBox;
import vn.pmgteam.kclient.auth.AuthManager;
import vn.pmgteam.kclient.auth.LoginAuthBox;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
    
    private final List<Popup> activePopups = new ArrayList<>();
    private final double baseX = 980; // góc phải (có thể chỉnh theo màn hình)
    private final double baseY = 660; // góc dưới
    private final double spacing = 0.0; // khoảng cách giữa các popup
    
    private static boolean debugMode = false;
    private static boolean verbose = false;
    private static boolean noSplash = false;
    
    // Ở đầu class Main, thêm:
    private StackPane overlay;

    
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
    	    ScaleTransition scale = new ScaleTransition(Duration.millis(300), root);
    	    scale.setToX(0);
    	    scale.setToY(0);
    	    scale.setInterpolator(Interpolator.EASE_IN);

    	    FadeTransition fade = new FadeTransition(Duration.millis(300), root);
    	    fade.setToValue(0);
    	    fade.setInterpolator(Interpolator.EASE_OUT);

    	    TranslateTransition slide = new TranslateTransition(Duration.millis(300), root);
    	    slide.setToY(200);
    	    slide.setInterpolator(Interpolator.EASE_IN);

    	    ParallelTransition pt = new ParallelTransition(scale, fade, slide);
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

    	versionSelect.setCellFactory(lv -> new ListCell<String>() {
    	    @Override
    	    protected void updateItem(String item, boolean empty) {
    	        super.updateItem(item, empty);
    	        if (empty || item == null) {
    	            setText(null);
    	            setStyle("");
    	        } else {
    	            setText(item);
    	            setTextFill(Color.WHITE);
    	            setStyle("-fx-background-color: #2a2a2a; -fx-border-radius: 6;");
    	            setOnMouseEntered(e -> setStyle("-fx-background-color: #3a3a3a; -fx-border-radius: 6;"));
    	            setOnMouseExited(e -> setStyle("-fx-background-color: #2a2a2a; -fx-border-radius: 6;"));
    	        }
    	    }
    	});

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
    	missionsPage.setStyle("-fx-background-color: #1e1e1e; -fx-background-radius: 15; -fx-border-radius: 15; -fx-border-color: #00aaff; -fx-border-width: 2;");

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
        modsPage.setStyle("-fx-background-color: #1e1e1e; -fx-background-radius: 15; -fx-border-radius: 15; -fx-border-color: #00aaff; -fx-border-width: 2;");

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
            card.setStyle("-fx-background-color: #36393f; -fx-background-radius: 8;");

            // --- Top bar (thumbnail + text) ---
            HBox topBarContainer = new HBox(10);
            topBarContainer.setAlignment(Pos.CENTER_LEFT);

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
                    // TODO: Download mod file from Modrinth
                } else {
                    installedMods.getItems().remove(modName);
                    installBtn.setText("Install");
                }
            });

            bottomBar.getChildren().addAll(downloads, installBtn);

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
    	VBox settingsPage = new VBox(20);
    	settingsPage.setAlignment(Pos.TOP_LEFT);
    	settingsPage.setPadding(new Insets(30));

    	Text settingsTitle = new Text(I18n.get("menu.settings"));
    	settingsTitle.setStyle("-fx-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");

    	HBox gamePathBox = new HBox(10);
    	Label gamePathLabel = new Label(I18n.get("settings.gamepath"));
    	gamePathLabel.setStyle("-fx-fill: white");
    	TextField gamePathField = new TextField(defaultGamePath);
    	Button browseBtn = new Button(I18n.get("settings.browse"));
    	gamePathBox.getChildren().addAll(gamePathLabel, gamePathField, browseBtn);

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

    	CheckBox autoUpdate = new CheckBox(I18n.get("settings.autoupdate"));
    	autoUpdate.setSelected(cfg.autoUpdate);

    	HBox themeBox = new HBox(10);
    	Label themeLabel = new Label(I18n.get("settings.theme"));
    	ComboBox<String> themeSelect = new ComboBox<>();
    	themeSelect.getItems().addAll(I18n.get("settings.theme.light"), I18n.get("settings.theme.dark"), I18n.get("settings.theme.system"));
    	themeSelect.setValue(cfg.theme);

    	themeBox.getChildren().addAll(themeLabel, themeSelect);

    	Button saveBtn = new Button(I18n.get("settings.save"));
    	saveBtn.setOnAction(e -> {
    	    cfg.gamePath = gamePathField.getText();
    	    cfg.autoUpdate = autoUpdate.isSelected();
    	    cfg.theme = themeSelect.getValue();

    	    // Save config
    	    configManager.save();

    	    this.showNotification(root, I18n.get("settings.saved"), 2000);

    	    // Update version list
    	    versionSelect.getItems().clear();
    	    versionSelect.getItems().addAll(loadMinecraftVersions(cfg.gamePath));
    	});

    	settingsPage.getChildren().addAll(settingsTitle, gamePathBox, autoUpdate, themeBox, saveBtn);
    	
    	// --- Artist Page ---


    	// --- Add pages to container ---
    	pagesContainer.getChildren().addAll(homePage, authPage, missionsPage, modsPage, settingsPage, artistPage);
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

    	Button settings = new Button(I18n.get("menu.settings"));
    	settings.getStyleClass().add("menu-button");
    	settings.setOnAction(e -> showPage(pagesContainer, settingsPage));

    	navBar.getChildren().addAll(home, accounts, missions, mods, settings);
    	layout.setBottom(navBar);

    	// === Background Image ===
    	String jsonUrl = "/backgrounds.json";
    	
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
    	        layout.setBackground(new Background(backgroundImage));
    	    }
    	} catch (Exception e) {
    	    e.printStackTrace();
    	}

    	// === ROOT container bo góc ===
    	root.setPadding(new Insets(8));
    	root.setStyle("-fx-background-color: rgba(30,41,59,0.92); -fx-background-radius: 16; -fx-border-radius: 16; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 20, 0, 0, 4);");

    	Scene scene = new Scene(root, 1280, 720);
    	scene.setFill(Color.TRANSPARENT);
    	scene.getStylesheets().add(getClass().getResource("/application/application.css").toExternalForm());

    	stage.initStyle(StageStyle.TRANSPARENT);
    	stage.setScene(scene);
    	stage.setTitle(I18n.get("app.title"));
    	stage.show();

    	root.setCache(true);
 
    	FadeTransition fade = new FadeTransition(Duration.millis(600), root);
    	fade.setFromValue(0);
    	fade.setToValue(1);
    	fade.setInterpolator(Interpolator.EASE_BOTH);

    	ParallelTransition pt = new ParallelTransition(fade);
    	pt.play();

    	addButtonAnimation(playButton);
    	addButtonAnimation(accounts);
    	addButtonAnimation(home);
    	addButtonAnimation(missions);
    	addButtonAnimation(mods);
    	addButtonAnimation(settings);
    	addButtonAnimation(minimize);
    	addButtonAnimation(close);

    	this.showNotification(root, I18n.get("nof.expiredlicense"), 3000);

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

    public static void main(String[] theNextWashingMachine) {
    	System.out.println("Starting....");
    	OptionParser parser = new OptionParser();

        parser.accepts("debug").withOptionalArg().ofType(Boolean.class);
        parser.accepts("verbose").withOptionalArg().ofType(Boolean.class);
        parser.accepts("nosplash").withOptionalArg().ofType(Boolean.class);

        OptionSet options = parser.parse(theNextWashingMachine);

        debugMode = options.has("debug");
        verbose = options.has("verbose");
        noSplash = options.has("nosplash");

        if (debugMode) System.out.println("[DEBUG] Debug mode enabled");
        if (verbose) System.out.println("[VERBOSE] Verbose logging enabled");
        if (noSplash) System.out.println("[INFO] Splash screen disabled");

        launch(theNextWashingMachine); // JavaFX start()
    }

	private void hide() {
	}
}
