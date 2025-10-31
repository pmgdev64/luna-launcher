package vn.pmgteam.luna;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.File;
import java.util.Locale;
import java.util.Stack;
import java.util.function.Consumer;

import application.Main;

public class OpenFileBox {

    private static double xOffset = 0;
    private static double yOffset = 0;
    public static Interpolator easeCapCut = Interpolator.SPLINE(0.25, 1, 0.5, 1);
    
    public OpenFileBox(File file)
    {
    	I18n.load(getClass(), "luna", new Locale("vi"));
    }

    public static void show(String title, File startFolder, Consumer<File> callback) {
        if (startFolder == null || !startFolder.isDirectory()) {
            callback.accept(null);
            return;
        }

        Stage stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);

        VBox root = new VBox(10);
        root.setPadding(new Insets(15));
        root.setAlignment(Pos.TOP_CENTER);
        root.setStyle(
                "-fx-background-color: rgba(40,40,40,0.95);" +
                "-fx-background-radius: 12;" +
                "-fx-border-radius: 12;" +
                "-fx-border-color: #00aaff;" +
                "-fx-border-width: 2;"
        );

        Text titleLabel = new Text(title);
        titleLabel.setFill(Color.WHITE);
        titleLabel.setFont(Font.font("Comic Relief", 16));

        // Breadcrumbs container
        HBox breadcrumbBox = new HBox(5);
        breadcrumbBox.setAlignment(Pos.CENTER_LEFT);

        ListView<HBox> folderList = new ListView<>();
        folderList.setPrefSize(500, 300);

        Stack<File> backHistory = new Stack<>();
        Stack<File> forwardHistory = new Stack<>();
        File[] currentFolder = {startFolder};

        // Load folder function
        final Runnable[] loadFolder = new Runnable[1];
        loadFolder[0] = () -> {
            folderList.getItems().clear();
            File folder = currentFolder[0];

            // Breadcrumbs
            breadcrumbBox.getChildren().clear();
            File temp = folder;
            Stack<File> tempStack = new Stack<>();
            while (temp != null) {
                tempStack.push(temp);
                temp = temp.getParentFile();
            }
            boolean first = true;
            while (!tempStack.isEmpty()) {
                File f = tempStack.pop();
                Button btn = new Button(f.getName().isEmpty() ? f.getAbsolutePath() : f.getName());
                btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #00aaff; -fx-font-size: 13;");
                btn.setOnAction(e -> {
                    backHistory.push(currentFolder[0]);
                    currentFolder[0] = f;
                    loadFolder[0].run(); // dùng loadFolder[0]
                });
                if (!first) breadcrumbBox.getChildren().add(new Label(">"));
                first = false;
                breadcrumbBox.getChildren().add(btn);
                btn.setFont(Font.font("Comic Relief"));
            }

            // Folder list
            File[] files = folder.listFiles(File::isDirectory);
            if (files != null) {
                for (File sub : files) {
                    HBox row = new HBox(5);
                    row.setAlignment(Pos.CENTER_LEFT);
                    ImageView icon = new ImageView();
                    icon.setFitHeight(16);
                    icon.setFitWidth(16);
                    Label nameLabel = new Label(sub.getName());
                    nameLabel.setTextFill(Color.BLACK);
                    row.getChildren().addAll(icon, nameLabel);

                    row.setOnMouseClicked(ev -> {
                        if (ev.getClickCount() == 2) { // double click
                            backHistory.push(currentFolder[0]);
                            currentFolder[0] = sub;
                            loadFolder[0].run(); // dùng loadFolder[0]
                        }
                    });
                    folderList.getItems().add(row);
                }
            }
        };

        // Load lần đầu
        loadFolder[0].run();

        // Control buttons
        Button selectBtn = new Button(I18n.get("openfile.title"));
        selectBtn.setStyle("-fx-background-color: #00aaff; -fx-text-fill: white; -fx-background-radius: 8;");
        selectBtn.setFont(Font.font("Comic Relief"));
        selectBtn.setOnAction(e -> {
            int idx = folderList.getSelectionModel().getSelectedIndex();
            if (idx >= 0 && idx < folderList.getItems().size()) {
                File folder = currentFolder[0].listFiles(File::isDirectory)[idx];
                closeWithZoom(stage, root, () -> callback.accept(folder));
            }
        });

        Button cancelBtn = new Button(I18n.get("login.cancel"));
        cancelBtn.setStyle("-fx-background-color: #888888; -fx-text-fill: white; -fx-background-radius: 8;");
        cancelBtn.setFont(Font.font("Comic Relief"));
        cancelBtn.setOnAction(e -> closeWithZoom(stage, root, () -> callback.accept(null)));

        Button backBtn = new Button(I18n.get("openfile.back"));
        backBtn.setStyle("-fx-background-color: #ffaa00; -fx-text-fill: white; -fx-background-radius: 8;");
        backBtn.setFont(Font.font("Comic Relief"));
        backBtn.setOnAction(e -> {
            if (!backHistory.isEmpty()) {
                forwardHistory.push(currentFolder[0]);
                currentFolder[0] = backHistory.pop();
                loadFolder[0].run();
            }
        });

        Button forwardBtn = new Button(I18n.get("openfile.forward"));
        forwardBtn.setStyle("-fx-background-color: #ffaa00; -fx-text-fill: white; -fx-background-radius: 8;");
        forwardBtn.setFont(Font.font("Comic Relief"));
        forwardBtn.setOnAction(e -> {
            if (!forwardHistory.isEmpty()) {
                backHistory.push(currentFolder[0]);
                currentFolder[0] = forwardHistory.pop();
                loadFolder[0].run();
            }
        });

        Button upBtn = new Button(I18n.get("openfile.up"));
        upBtn.setStyle("-fx-background-color: #ffaa00; -fx-text-fill: white; -fx-background-radius: 8;");
        upBtn.setFont(Font.font("Comic Relief"));
        upBtn.setOnAction(e -> {
            File parent = currentFolder[0].getParentFile();
            if (parent != null) {
                backHistory.push(currentFolder[0]);
                currentFolder[0] = parent;
                loadFolder[0].run();
            }
        });

        HBox navBox = new HBox(8, backBtn, forwardBtn, upBtn);
        navBox.setAlignment(Pos.CENTER);

        root.getChildren().addAll(titleLabel, breadcrumbBox, folderList, navBox, selectBtn, cancelBtn);

        // Drag support
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
        stage.setTitle(title);
        stage.getIcons().add(Main.titleIcon);

        // Zoom in effect
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

    private static void closeWithZoom(Stage stage, VBox root, Runnable onFinished) {
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
        zoomOut.setOnFinished(ev -> {
            stage.close();
            if (onFinished != null) onFinished.run();
        });
        zoomOut.play();
    }
}
