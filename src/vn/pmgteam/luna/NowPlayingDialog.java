package vn.pmgteam.luna;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.net.URL;

public class NowPlayingDialog {

    private static double xOffset = 0;
    private static double yOffset = 0;
    public static Interpolator easeCapCut = Interpolator.SPLINE(0.25, 1, 0.5, 1);

    private static boolean isPlaying = true;
    private static Timeline progressTimeline;

    public static void show(String title, String artist, String coverUrl) {
        Stage stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setAlwaysOnTop(true);

        VBox root = new VBox(10);
        root.setPadding(new Insets(14));
        root.setAlignment(Pos.CENTER_LEFT);
        root.setPrefWidth(380);
        root.setStyle(
                "-fx-background-color: rgba(30,30,30,0.85);" +
                "-fx-background-radius: 16;" +
                "-fx-border-color: rgba(255,255,255,0.15);" +
                "-fx-border-radius: 16;" +
                "-fx-border-width: 1;"
        );
        root.setEffect(new DropShadow(20, Color.color(0, 0, 0, 0.6)));

        // === Top bar ===
        HBox windowBar = new HBox(6);
        windowBar.setAlignment(Pos.CENTER_RIGHT);
        windowBar.setPadding(new Insets(0, 0, 6, 0));

        Button minimizeBtn = new Button("—");
        Button closeBtn = new Button("×");
        
        minimizeBtn.setFont(Font.font("Comic Relief", 16));
        minimizeBtn.setTextFill(Color.WHITE);
        minimizeBtn.setBackground(Background.EMPTY);
        minimizeBtn.setBorder(Border.EMPTY);
        minimizeBtn.setPadding(new Insets(2, 8, 2, 8));
        minimizeBtn.setOnMouseEntered(e -> minimizeBtn.setTextFill(Color.web("#ffaa00")));
        minimizeBtn.setOnMouseExited(e -> minimizeBtn.setTextFill(Color.WHITE));
        minimizeBtn.setCursor(javafx.scene.Cursor.HAND);
        
        closeBtn.setFont(Font.font("Comic Relief", 16));
        closeBtn.setTextFill(Color.WHITE);
        closeBtn.setBackground(Background.EMPTY);
        closeBtn.setBorder(Border.EMPTY);
        closeBtn.setPadding(new Insets(2, 8, 2, 8));
        closeBtn.setOnMouseEntered(e -> minimizeBtn.setTextFill(Color.web("#ffaa00")));
        closeBtn.setOnMouseExited(e -> minimizeBtn.setTextFill(Color.WHITE));
        closeBtn.setCursor(javafx.scene.Cursor.HAND);

        for (Button btn : new Button[]{minimizeBtn, closeBtn}) {
            btn.setFont(Font.font("Comic Relief", 16));
            btn.setTextFill(Color.WHITE);
            btn.setBackground(Background.EMPTY);
            btn.setBorder(Border.EMPTY);
            btn.setPadding(new Insets(2, 8, 2, 8));
            btn.setOnMouseEntered(e -> btn.setTextFill(Color.web("#ffaa00")));
            btn.setOnMouseExited(e -> btn.setTextFill(Color.WHITE));
            btn.setCursor(javafx.scene.Cursor.HAND);
        }

        minimizeBtn.setOnAction(e -> stage.setIconified(true));
        closeBtn.setOnAction(e -> close(stage, root));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        windowBar.getChildren().addAll(spacer, minimizeBtn, closeBtn);

        // === Cover ===
        ImageView cover = new ImageView();
        cover.setFitWidth(96);
        cover.setFitHeight(96);
        cover.setPreserveRatio(true);
        cover.setSmooth(true);
        try {
            cover.setImage(new Image(new URL(coverUrl).openStream()));
        } catch (Exception eIn) {
            eIn.printStackTrace();
        }

        // === Text Info ===
        Label titleLabel = new Label(title);
        titleLabel.setTextFill(Color.WHITE);
        titleLabel.setFont(Font.font("Comic Relief", 16));

        Label artistLabel = new Label(artist);
        artistLabel.setTextFill(Color.web("#CCCCCC"));
        artistLabel.setFont(Font.font("Comic Relief", 13));

        VBox infoBox = new VBox(titleLabel, artistLabel);
        infoBox.setSpacing(4);
        infoBox.setAlignment(Pos.CENTER_LEFT);

        HBox topBox = new HBox(14, cover, infoBox);
        topBox.setAlignment(Pos.CENTER_LEFT);

        // === Controls ===
        Button prevBtn = new Button("⏮");
        Button playPauseBtn = new Button("⏯");
        Button nextBtn = new Button("⏭");

        for (Button b : new Button[]{prevBtn, playPauseBtn, nextBtn}) {
            b.setFont(Font.font("Comic Relief", 16));
            b.setTextFill(Color.WHITE);
            b.setBackground(Background.EMPTY);
            b.setBorder(Border.EMPTY);
            b.setOnMouseEntered(e -> b.setTextFill(Color.web("#ffaa00")));
            b.setOnMouseExited(e -> b.setTextFill(Color.WHITE));
            b.setCursor(javafx.scene.Cursor.HAND);
            b.getStyleClass().add("dialog-button");
        }

        playPauseBtn.setOnAction(e -> {
            isPlaying = !isPlaying;
            playPauseBtn.setText(isPlaying ? "⏯" : "▶");
            if (isPlaying) progressTimeline.play();
            else progressTimeline.pause();
        });

        HBox controls = new HBox(16, prevBtn, playPauseBtn, nextBtn);
        controls.setAlignment(Pos.CENTER);

        // === Progress Bar ===
        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(340);
        progressBar.setStyle("-fx-accent: #ffaa00;");

        progressTimeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(progressBar.progressProperty(), 0)),
                new KeyFrame(Duration.seconds(30), new KeyValue(progressBar.progressProperty(), 1))
        );
        progressTimeline.setCycleCount(Animation.INDEFINITE);
        progressTimeline.play();

        root.getChildren().addAll(windowBar, topBox, progressBar, controls);

        // === Drag window ===
        root.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        root.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });

        // === Scene ===
        Scene scene = new Scene(root);
        scene.getStylesheets().add(NowPlayingDialog.class.getResource("/application/application.css").toExternalForm());
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);

        // === Zoom-in Animation ===
        root.setOpacity(0);
        root.setScaleX(0.2);
        root.setScaleY(0.2);
        stage.show();

        Timeline zoomIn = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(root.opacityProperty(), 0),
                        new KeyValue(root.scaleXProperty(), 0.2),
                        new KeyValue(root.scaleYProperty(), 0.2)
                ),
                new KeyFrame(Duration.millis(320),
                        new KeyValue(root.opacityProperty(), 1, easeCapCut),
                        new KeyValue(root.scaleXProperty(), 1, easeCapCut),
                        new KeyValue(root.scaleYProperty(), 1, easeCapCut)
                ),
                new KeyFrame(Duration.millis(500),
                        new KeyValue(root.scaleXProperty(), 0.94, Interpolator.EASE_IN),
                        new KeyValue(root.scaleYProperty(), 0.94, Interpolator.EASE_IN)
                )
        );
        zoomIn.play();

        // Auto close after track ends (optional)
        progressTimeline.setOnFinished(e -> close(stage, root));
    }

    private static void close(Stage stage, VBox root) {
        Timeline zoomOut = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(root.opacityProperty(), 1),
                        new KeyValue(root.scaleXProperty(), 1),
                        new KeyValue(root.scaleYProperty(), 1)
                ),
                new KeyFrame(Duration.millis(400),
                        new KeyValue(root.opacityProperty(), 0, easeCapCut),
                        new KeyValue(root.scaleXProperty(), 0.2, easeCapCut),
                        new KeyValue(root.scaleYProperty(), 0.2, easeCapCut)
                )
        );
        zoomOut.setOnFinished(ev -> stage.close());
        zoomOut.play();
    }
}
