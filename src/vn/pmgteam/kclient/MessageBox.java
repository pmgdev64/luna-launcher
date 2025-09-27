package vn.pmgteam.kclient;

import application.Main;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class MessageBox {

    private static double xOffset = 0;
    private static double yOffset = 0;
    
    public static Interpolator easeCapCut = Interpolator.SPLINE(0.30, 1, 0.5, 1);

    public static void showError(String message) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.TRANSPARENT);

        VBox root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));
        root.setStyle(
            "-fx-background-color: rgba(50,50,50,0.9);" +
            "-fx-background-radius: 12;" +
            "-fx-border-radius: 12;" +
            "-fx-border-color: #ff5555;" +
            "-fx-border-width: 2;"
        );

        // --- Message Label ---
        Label msg = new Label(message);
        msg.setTextFill(Color.WHITE);
        msg.setFont(Font.font("Segoe UI", 14));
        msg.setWrapText(true);

        // --- OK Button ---
        Button ok = new Button("OK");
        ok.setStyle(
            "-fx-background-color: #ff5555;" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 8;"
        );
        ok.setOnAction(e -> {
            // Fade out khi đóng
            FadeTransition ftOut = new FadeTransition(Duration.millis(200), root);
            Timeline zoomOut = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(root.opacityProperty(), 1),
                            new KeyValue(root.scaleXProperty(), 0.94),
                            new KeyValue(root.scaleYProperty(), 0.94)
                    ),
                    new KeyFrame(Duration.millis(400),
                            new KeyValue(root.opacityProperty(), 0, easeCapCut),
                            new KeyValue(root.scaleXProperty(), 0.2, easeCapCut),
                            new KeyValue(root.scaleYProperty(), 0.2, easeCapCut)
                    )
            );
            ftOut.setFromValue(1.0);
            ftOut.setToValue(0.0);
            ftOut.setOnFinished(ev -> stage.close());
            ftOut.play();
            zoomOut.play();
        });

        root.getChildren().addAll(msg, ok);

        // --- Make draggable ---
        root.setOnMousePressed((MouseEvent event) -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        root.setOnMouseDragged((MouseEvent event) -> {
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
        stage.getIcons().add(Main.titleIcon);

        // --- Fade in ---
        root.setOpacity(0.0);
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
                new KeyFrame(Duration.millis(500),
                        new KeyValue(root.opacityProperty(), 1),
                        new KeyValue(root.scaleXProperty(), 0.94, Interpolator.EASE_IN),
                        new KeyValue(root.scaleYProperty(), 0.94, Interpolator.EASE_IN)
                )
        );
        FadeTransition ftIn = new FadeTransition(Duration.millis(300), root);
        ftIn.setFromValue(0.0);
        ftIn.setToValue(1.0);
        ftIn.play();
        zoomIn.play();
    }
}
