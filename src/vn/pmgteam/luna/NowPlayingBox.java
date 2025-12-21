package vn.pmgteam.luna;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Popup;
import javafx.util.Duration;

public class NowPlayingBox {
    private Popup popup;
    private Label titleLabel;
    public ProgressBar progressBar;

    public NowPlayingBox(StackPane parent) {
        popup = new Popup();

        VBox content = new VBox(6);
        content.setStyle(
            "-fx-background-color: #222222;" +
            "-fx-background-radius: 12;" +
            "-fx-padding: 12;" +
            "-fx-border-radius: 12;" +
            "-fx-border-color: #55ff55;" +
            "-fx-border-width: 2;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 6, 0, 0, 2);"
        );

        titleLabel = new Label("Now Playing: -");
        titleLabel.setTextFill(Color.WHITE);
        titleLabel.setFont(new Font(14));

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(200);

        content.getChildren().addAll(titleLabel, progressBar);
        popup.getContent().add(content);

        // Chạy show + animation trong Platform.runLater để tránh crash
        javafx.application.Platform.runLater(() -> {
            popup.show(parent.getScene().getWindow(), 20, 500);

            content.setTranslateY(50);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), content);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);

            TranslateTransition slideIn = new TranslateTransition(Duration.millis(300), content);
            slideIn.setFromY(50);
            slideIn.setToY(0);
            slideIn.setInterpolator(Interpolator.EASE_OUT);

            new ParallelTransition(fadeIn, slideIn).play();
        });
    }

    public void update(String song, double progress) {
        titleLabel.setText("Now Playing: " + song);
        progressBar.setProgress(progress);
    }

    public void hide() {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), popup.getContent().get(0));
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);

        TranslateTransition slideOut = new TranslateTransition(Duration.millis(300), popup.getContent().get(0));
        slideOut.setFromY(0);
        slideOut.setToY(50);

        ParallelTransition disappear = new ParallelTransition(fadeOut, slideOut);
        disappear.setOnFinished(e -> popup.hide());
        disappear.play();
    }
}
