package vn.pmgteam.kclient.auth;

import java.util.Locale;
import java.util.function.BiConsumer;

import application.Main;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import vn.pmgteam.kclient.I18n;
import vn.pmgteam.kclient.MessageBox;

public class LoginAuthBox {

    private static double xOffset = 0;
    private static double yOffset = 0;

    public static Button cancelBtn = new Button(I18n.get("login.cancel"));
    private static Runnable onCancel;

    public static Interpolator easeCapCut = Interpolator.SPLINE(0.25, 1, 0.5, 1);
    
    public LoginAuthBox(String Lang, String Locate)
    {
    	I18n.load(getClass(), "luna", new Locale("vi"));
    }

    /** Đặt callback khi người dùng nhấn Cancel */
    public static void setOnCancel(Runnable r) {
        onCancel = r;
    }

    public static void show(BiConsumer<String, String> callback) {
        Stage stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);

        VBox root = new VBox(14);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);
        root.setStyle(
                "-fx-background-color: rgba(40,40,40,0.95);" +
                "-fx-background-radius: 12;" +
                "-fx-border-radius: 12;" +
                "-fx-border-color: #00aaff;" +
                "-fx-border-width: 2;"
        );

        Text title = new Text(I18n.get("login.title"));
        title.setFont(Font.font("Segoe UI", 18));
        title.setFill(Color.WHITE);

        // StackPane để chứa các form login
        StackPane formContainer = new StackPane();
        formContainer.setPrefHeight(180);

        // === Offline Login ===
        VBox offlineForm = new VBox(8);
        offlineForm.setAlignment(Pos.CENTER);
        TextField offlineUser = new TextField();
        offlineUser.setPromptText(I18n.get("offline.username"));
        styleField(offlineUser);

        Button offlineBtn = new Button(I18n.get("offline.loginbutton"));
        styleMainButton(offlineBtn);
        offlineBtn.setOnAction(e -> {
            closeWithZoom(stage, root, () -> {
                if (callback != null)
                    callback.accept("offline", offlineUser.getText());
            });
        });
        offlineForm.getChildren().addAll(new Text(I18n.get("offline.mode")), offlineUser, offlineBtn);

        // === Local Login ===
        VBox localForm = new VBox(8);
        localForm.setAlignment(Pos.CENTER);
        TextField localUser = new TextField();
        localUser.setPromptText(I18n.get("local.username"));
        styleField(localUser);
        PasswordField localPass = new PasswordField();
        localPass.setPromptText(I18n.get("local.password"));
        styleField(localPass);

        Button localBtn = new Button(I18n.get("local.login"));
        styleMainButton(localBtn);
        localBtn.setOnAction(e -> {
            closeWithZoom(stage, root, () -> {
                if (callback != null)
                    callback.accept("local", localUser.getText() + ":" + localPass.getText());
            });
        });
        localForm.getChildren().addAll(new Text(I18n.get("local.auth")), localUser, localPass, localBtn);

        // === Ten_Auth Login ===
        VBox tenAuthForm = new VBox(8);
        tenAuthForm.setAlignment(Pos.CENTER);
        TextField tenUser = new TextField();
        tenUser.setPromptText(I18n.get("yourauth.username"));
        styleField(tenUser);
        PasswordField tenPass = new PasswordField();
        tenPass.setPromptText(I18n.get("yourauth.password"));
        styleField(tenPass);

        Button tenAuthBtn = new Button(I18n.get("yourauth.login"));
        styleMainButton(tenAuthBtn);
        tenAuthBtn.setOnAction(e -> {
            closeWithZoom(stage, root, () -> {
                if (callback != null)
                    callback.accept("ten_auth", tenUser.getText() + ":" + tenPass.getText());
            });
        });
        tenAuthForm.getChildren().addAll(new Text(I18n.get("yourauth.mode")), tenUser, tenPass, tenAuthBtn);

        // === Microsoft Login ===
        VBox msForm = new VBox(8);
        msForm.setAlignment(Pos.CENTER);
        Button msBtn = new Button(I18n.get("microsoft.login"));
        msBtn.setStyle("-fx-background-color: #2d89ef; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8;");
        msBtn.setOnAction(e -> {
            MessageBox.showError(I18n.get("microsoft.noavailable"));
            // sau này thay bằng AuthManager.loginMicrosoft();
        });
        msForm.getChildren().addAll(new Text("Microsoft OAuth"), msBtn);

        // Thêm tất cả form vào StackPane
        formContainer.getChildren().addAll(offlineForm, localForm, tenAuthForm, msForm);
        offlineForm.setVisible(true);
        localForm.setVisible(false);
        tenAuthForm.setVisible(false);
        msForm.setVisible(false);

        // === Buttons để chọn method ===
        Button offlineTab = new Button(I18n.get("offline.title"));
        Button localTab = new Button(I18n.get("local.title"));
        Button tenTab = new Button(I18n.get("yourauth.title"));
        Button msTab = new Button(I18n.get("microsoft.title"));

        HBox methodRow = new HBox(10, offlineTab, localTab, tenTab, msTab);
        methodRow.setAlignment(Pos.CENTER);

        styleTabButton(offlineTab);
        styleTabButton(localTab);
        styleTabButton(tenTab);
        styleTabButton(msTab);

        offlineTab.setOnAction(e -> switchForm(offlineForm, localForm, tenAuthForm, msForm));
        localTab.setOnAction(e -> switchForm(localForm, offlineForm, tenAuthForm, msForm));
        tenTab.setOnAction(e -> switchForm(tenAuthForm, offlineForm, localForm, msForm));
        msTab.setOnAction(e -> switchForm(msForm, offlineForm, localForm, tenAuthForm));

        // === Cancel ===
        cancelBtn.setStyle("-fx-background-color: #888; -fx-text-fill: white; -fx-background-radius: 8;");
        cancelBtn.setOnAction(e -> closeWithZoom(stage, root, () -> {
            if (onCancel != null) onCancel.run();
        }));

        root.getChildren().addAll(title, methodRow, formContainer, cancelBtn);

        // Drag window
        root.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        root.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
        stage.setTitle("Login Or Sign Up");
        stage.getIcons().add(Main.titleIcon);

        // Zoom-in animation
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
                new KeyFrame(Duration.millis(500),
                        new KeyValue(root.opacityProperty(), 1),
                        new KeyValue(root.scaleXProperty(), 0.94, Interpolator.EASE_IN),
                        new KeyValue(root.scaleYProperty(), 0.94, Interpolator.EASE_IN)
                )
        );
        zoomIn.play();
    }

    private static void switchForm(VBox show, VBox... hide) {
        show.setVisible(true);
        for (VBox h : hide) h.setVisible(false);
    }

    private static void styleField(TextField field) {
        field.setStyle("-fx-background-radius: 8; -fx-background-color: #2a2a2a; -fx-text-fill: white;");
    }

    private static void styleMainButton(Button btn) {
        btn.setStyle("-fx-background-color: #00aaff; -fx-text-fill: white; -fx-background-radius: 8;");
    }

    private static void styleTabButton(Button btn) {
        btn.setStyle("-fx-background-color: #444; -fx-text-fill: white; -fx-background-radius: 6;");
    }

    private static void closeWithZoom(Stage stage, VBox root, Runnable onFinished) {
        Timeline zoomOut = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(root.opacityProperty(), 1),
                        new KeyValue(root.scaleXProperty(), 1),
                        new KeyValue(root.scaleYProperty(), 1)
                ),
                new KeyFrame(Duration.millis(300),
                        new KeyValue(root.opacityProperty(), 0, easeCapCut),
                        new KeyValue(root.scaleXProperty(), 0.2, easeCapCut),
                        new KeyValue(root.scaleYProperty(), 0.2, easeCapCut)
                )
        );
        zoomOut.setOnFinished(ev -> {
            stage.close();
            if (onFinished != null) onFinished.run();
        });
        zoomOut.play();
    }
}
