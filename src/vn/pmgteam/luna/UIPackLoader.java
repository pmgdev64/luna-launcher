package vn.pmgteam.luna;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

import javafx.scene.Parent;

public class UIPackLoader {
    private final File jarFile;
    private final URLClassLoader classLoader;

    public UIPackLoader(File jarFile) throws Exception {
        this.jarFile = jarFile;
        this.classLoader = new URLClassLoader(new URL[]{ jarFile.toURI().toURL() });
    }

    public URLClassLoader getClassLoader() {
        return classLoader;
    }

    // Load layout class, trả về Parent (Pane, VBox, etc)
    public Parent loadLayout(String className) throws Exception {
        Class<?> clazz = classLoader.loadClass(className);
        Object instance = clazz.getDeclaredConstructor().newInstance();
        if (instance instanceof Parent) return (Parent) instance;
        throw new RuntimeException("Layout class phải kế thừa Parent");
    }

    // Load overlay config (ví dụ JSON)
    public OverlayConfig loadOverlay(String fileName) {
        // Có thể parse JSON từ jar, fallback mặc định
        return new OverlayConfig();
    }
}
