package vn.pmgteam.luna;

import java.time.LocalDateTime;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.stage.Popup;
import javafx.util.Duration; // <-- đây là Duration của JavaFX

public class CountdownBox {
    private Popup popup;
    private Label countdownLabel;
    private Timeline timer;

    public CountdownBox(StackPane parent, LocalDateTime targetTime) {
        popup = new Popup();

        countdownLabel = new Label();
        countdownLabel.setStyle(
            "-fx-background-color: #333333;" +
            "-fx-text-fill: #ffdd55;" +
            "-fx-padding: 12px 20px;" +
            "-fx-background-radius: 12;" +
            "-fx-border-radius: 12;" +
            "-fx-border-color: #ffaa00;" +
            "-fx-border-width: 2;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 6, 0, 0, 2);" +
            "-fx-font-size: 16px;"
        );
        countdownLabel.setFont(new Font(16));

        popup.getContent().add(countdownLabel);
        popup.show(parent.getScene().getWindow(), 20, 400);

        // Slide + fade-in
        countdownLabel.setTranslateY(50);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), countdownLabel);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        TranslateTransition slideIn = new TranslateTransition(Duration.millis(300), countdownLabel);
        slideIn.setFromY(50);
        slideIn.setToY(0);
        slideIn.setInterpolator(Interpolator.EASE_OUT);

        new ParallelTransition(fadeIn, slideIn).play();

        // Timer cập nhật mỗi giây (sử dụng javafx.util.Duration)
        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateCountdown(targetTime)));
        timer.setCycleCount(Animation.INDEFINITE);
        timer.play();
    }

    private void updateCountdown(LocalDateTime target) {
        java.time.Duration duration = java.time.Duration.between(LocalDateTime.now(), target);
        long totalSeconds = Math.max(0, duration.getSeconds());

        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        countdownLabel.setText(String.format("Countdown: %02d:%02d:%02d:%02d", days, hours, minutes, seconds));
    }

    public void hide() {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), countdownLabel);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);

        TranslateTransition slideOut = new TranslateTransition(Duration.millis(300), countdownLabel);
        slideOut.setFromY(0);
        slideOut.setToY(50);

        ParallelTransition disappear = new ParallelTransition(fadeOut, slideOut);
        disappear.setOnFinished(e -> popup.hide());
        disappear.play();

        timer.stop();
    }
    
    public double getWidth() {
        return countdownLabel.getWidth() + 4; // +4 để tránh tràn biên
    }

    public double getHeight() {
        return countdownLabel.getHeight() + 4;
    }

    public void setX(double x) {
        popup.setX(x);
    }

    public void setY(double y) {
        popup.setY(y);
    }

    public void setPosition(double x, double y) {
        setX(x);
        setY(y);
    }
}
