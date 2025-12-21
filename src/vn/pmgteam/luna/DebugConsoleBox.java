package vn.pmgteam.luna;

import application.Main;
import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import vn.pmgteam.luna.I18n;

public class DebugConsoleBox {

    private static Stage stage;
    private static TextArea console;
    private static VBox root;

    private static double xOffset;
    private static double yOffset;

    public static Interpolator easeCapCut =
            Interpolator.SPLINE(0.25, 1, 0.5, 1);

    // ===============================
    // SHOW
    // ===============================
    public static void show() {
        if (stage != null && stage.isShowing()) return;

        stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);

        root = new VBox(12);
        root.setPadding(new Insets(16));
        root.setAlignment(Pos.CENTER);
        root.setStyle(
                "-fx-background-color: rgba(20,20,20,0.95);" +
                "-fx-background-radius: 12;" +
                "-fx-border-radius: 12;" +
                "-fx-border-color: #ff5555;" +
                "-fx-border-width: 2;"
        );

        // ===== Title =====
        Text title = new Text("Debug Console");
        title.setFont(Font.font("Segoe UI", 18));
        title.setFill(Color.WHITE);

        // ===== Console =====
        console = new TextArea();
        console.setEditable(false);
        console.setWrapText(true);
        console.setStyle(
                "-fx-control-inner-background: #0f0f0f;" +
                "-fx-font-family: Consolas;" +
                "-fx-font-size: 12px;" +
                "-fx-text-fill: #00ff99;" +
                "-fx-highlight-fill: #444;"
        );

        ScrollPane scroll = new ScrollPane(console);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setPrefSize(520, 280);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        // ===== Buttons =====
        Button clearBtn = new Button("Clear");
        Button closeBtn = new Button(I18n.get("close", "Close"));

        styleButton(clearBtn, "#ffaa00");
        styleButton(closeBtn, "#888888");

        clearBtn.setOnAction(e -> console.clear());
        closeBtn.setOnAction(e -> close());

        HBox buttons = new HBox(10, clearBtn, closeBtn);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(title, scroll, buttons);

        // ===== Drag window =====
        root.setOnMousePressed(e -> {
            xOffset = e.getSceneX();
            yOffset = e.getSceneY();
        });
        root.setOnMouseDragged(e -> {
            stage.setX(e.getScreenX() - xOffset);
            stage.setY(e.getScreenY() - yOffset);
        });

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
        stage.setTitle("Debug Console");
        stage.getIcons().add(Main.titleIcon);

        // ===== Zoom in =====
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
                new KeyFrame(Duration.millis(300),
                        new KeyValue(root.opacityProperty(), 1, easeCapCut),
                        new KeyValue(root.scaleXProperty(), 1, easeCapCut),
                        new KeyValue(root.scaleYProperty(), 1, easeCapCut)
                ),
                new KeyFrame(Duration.millis(450),
                        new KeyValue(root.scaleXProperty(), 0.96),
                        new KeyValue(root.scaleYProperty(), 0.96)
                )
        );
        zoomIn.play();
    }

    // ===============================
    // APPEND LOG (THREAD-SAFE)
    // ===============================
    public static void append(String text) {
        if (console == null) return;

        console.appendText(text + "\n");
        console.positionCaret(console.getLength());
    }

    // ===============================
    // CLOSE WITH ANIMATION
    // ===============================
    public static void close() {
        if (stage == null) return;

        Timeline zoomOut = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(root.opacityProperty(), 1),
                        new KeyValue(root.scaleXProperty(), 1),
                        new KeyValue(root.scaleYProperty(), 1)
                ),
                new KeyFrame(Duration.millis(250),
                        new KeyValue(root.opacityProperty(), 0, easeCapCut),
                        new KeyValue(root.scaleXProperty(), 0.2, easeCapCut),
                        new KeyValue(root.scaleYProperty(), 0.2, easeCapCut)
                )
        );

        zoomOut.setOnFinished(e -> stage.close());
        zoomOut.play();
    }

    // ===============================
    // STYLE
    // ===============================
    private static void styleButton(Button btn, String color) {
        btn.setStyle(
                "-fx-background-color: " + color + ";" +
                "-fx-text-fill: white;" +
                "-fx-background-radius: 8;"
        );
    }
}
