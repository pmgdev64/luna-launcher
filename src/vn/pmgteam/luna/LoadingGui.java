package vn.pmgteam.luna;

import java.util.Locale;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.control.ProgressBar;
import javafx.util.Duration;

public class LoadingGui {

    private final StackPane parent;
    private final BorderPane overlay;

    private final ProgressBar progressBar;
    private final Text statusText;
    private final Text percentText;

    public static final Interpolator easeCapCut = Interpolator.SPLINE(0.30, 1, 0.5, 1);

    public LoadingGui(StackPane parent) {
        this.parent = parent;

        overlay = new BorderPane();

        I18n.load(getClass(), "luna", new Locale("vi"));
        // Background image
        try {
            Image bg = new Image(getClass().getResource("/resources/loadbg.jpg").toExternalForm());
            BackgroundImage bgImage = new BackgroundImage(
                    bg,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundPosition.CENTER,
                    new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, true, true)
            );
            overlay.setBackground(new Background(bgImage));
        } catch (Exception e) {
            overlay.setStyle("-fx-background-color: black;");
        }

        // Bottom panel
        VBox bottomPane = new VBox(8);
        bottomPane.setAlignment(Pos.CENTER_LEFT);
        bottomPane.setStyle("-fx-background-color: white;");
        bottomPane.setPadding(new javafx.geometry.Insets(20));

        Text loadingText = new Text(I18n.get("loading.title"));
        loadingText.setFont(Font.font("Comic Relief", 18));
        loadingText.setFill(Color.BLACK);

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(400);
        progressBar.setStyle("-fx-accent: #000000;");

        statusText = new Text("Initializing...");
        statusText.setFont(Font.font("Comic Relief", 14));
        statusText.setFill(Color.BLACK);

        percentText = new Text("0%");
        percentText.setFont(Font.font("Comic Relief", 14));
        percentText.setFill(Color.BLACK);

        HBox progressBox = new HBox(10, progressBar, percentText);
        progressBox.setAlignment(Pos.CENTER_LEFT);

        bottomPane.getChildren().addAll(loadingText, progressBox, statusText);
        overlay.setBottom(bottomPane);

        overlay.setOpacity(0);
        overlay.setScaleX(0.2);
        overlay.setScaleY(0.2);
    }

    public void show() {
        if (!parent.getChildren().contains(overlay)) parent.getChildren().add(overlay);

        FadeTransition fade = new FadeTransition(Duration.millis(300), overlay);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setInterpolator(easeCapCut);

        ScaleTransition scale = new ScaleTransition(Duration.millis(300), overlay);
        scale.setFromX(0.2);
        scale.setFromY(0.2);
        scale.setToX(1);
        scale.setToY(1);
        scale.setInterpolator(easeCapCut);

        ParallelTransition inAnim = new ParallelTransition(fade, scale);
        inAnim.play();
    }

    public void hide() {
        hide(null);
    }

    public void hide(Runnable onComplete) {
        FadeTransition fade = new FadeTransition(Duration.millis(300), overlay);
        fade.setFromValue(1);
        fade.setToValue(0);
        fade.setInterpolator(easeCapCut);

        ScaleTransition scale = new ScaleTransition(Duration.millis(300), overlay);
        scale.setFromX(1);
        scale.setFromY(1);
        scale.setToX(0.2);
        scale.setToY(0.2);
        scale.setInterpolator(easeCapCut);

        ParallelTransition outAnim = new ParallelTransition(fade, scale);
        outAnim.setOnFinished(e -> {
            parent.getChildren().remove(overlay);
            if (onComplete != null) onComplete.run();
        });
        outAnim.play();
    }

    public void setProgress(double progress, String status) {
        Platform.runLater(() -> {
            double current = progressBar.getProgress();

            Timeline t = new Timeline(
                new KeyFrame(Duration.ZERO,
                    new KeyValue(progressBar.progressProperty(), current)
                ),
                new KeyFrame(Duration.millis(500),
                    new KeyValue(progressBar.progressProperty(), progress, easeCapCut)
                )
            );
            t.play();

            SimpleDoubleProperty animatedValue = new SimpleDoubleProperty(current);
            animatedValue.addListener((obs, oldVal, newVal) -> {
                percentText.setText((int)(newVal.doubleValue() * 100) + "%");
            });

            Timeline t2 = new Timeline(
                new KeyFrame(Duration.ZERO,
                    new KeyValue(animatedValue, current)
                ),
                new KeyFrame(Duration.millis(500),
                    new KeyValue(animatedValue, progress, easeCapCut)
                )
            );
            t2.play();

            statusText.setText(status);
        });
    }


    public ProgressListener getProgressListener() {
        return this::setProgress;
    }
}
