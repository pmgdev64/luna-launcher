package vn.pmgteam.luna;

import javafx.scene.*;
import javafx.scene.image.Image;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.paint.Color;
import javafx.scene.shape.Box;
import javafx.scene.transform.Rotate;

import java.io.File;

public class MinecraftSkinViewer {

    private final Group root3D = new Group();

    public MinecraftSkinViewer(File skinFile) {
        Image skin = new Image(skinFile.toURI().toString());

        // Head 8x8x8
        Box head = createBox(8, 8, 8, skin, 8, 8);
        head.setTranslateY(-16);

        // Body 8x12x4
        Box body = createBox(8, 12, 4, skin, 20, 20);
        body.setTranslateY(-6);

        // Arms 4x12x4
        Box leftArm = createBox(4, 12, 4, skin, 44, 20);
        leftArm.setTranslateX(-6);
        leftArm.setTranslateY(-6);

        Box rightArm = createBox(4, 12, 4, skin, 44, 20);
        rightArm.setTranslateX(6);
        rightArm.setTranslateY(-6);

        // Legs 4x12x4
        Box leftLeg = createBox(4, 12, 4, skin, 4, 20);
        leftLeg.setTranslateX(-2);
        leftLeg.setTranslateY(6);

        Box rightLeg = createBox(4, 12, 4, skin, 4, 20);
        rightLeg.setTranslateX(2);
        rightLeg.setTranslateY(6);

        root3D.getChildren().addAll(head, body, leftArm, rightArm, leftLeg, rightLeg);

        // Xoay toàn bộ char
        root3D.getTransforms().add(new Rotate(-20, Rotate.X_AXIS));
        root3D.getTransforms().add(new Rotate(180, Rotate.Y_AXIS));
    }

    private Box createBox(double width, double height, double depth, Image skin, double u, double v) {
        Box box = new Box(width, height, depth);
        PhongMaterial mat = new PhongMaterial();
        mat.setDiffuseMap(skin);
        box.setMaterial(mat);
        return box;
    }

    public Node getNode() {
        SubScene subScene = new SubScene(root3D, 300, 400, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.rgb(30, 30, 30));

        PerspectiveCamera cam = new PerspectiveCamera(true);
        cam.setTranslateZ(-50);
        cam.setNearClip(0.1);
        cam.setFarClip(1000);
        subScene.setCamera(cam);

        return subScene;
    }
}
