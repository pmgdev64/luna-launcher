package vn.pmgteam.luna;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javafx.scene.Node;
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

    /**
     * Load layout class, trả về Parent (Pane, VBox, etc)
     */
    public Parent loadLayout(String className) throws Exception {
        Class<?> clazz = classLoader.loadClass(className);
        Object instance = clazz.getDeclaredConstructor().newInstance();
        if (instance instanceof Parent) return (Parent) instance;
        throw new RuntimeException("Layout class phải kế thừa Parent");
    }

    /**
     * Load một Node (page) từ jar, ví dụ ui.pages.HomePage
     */
    public Node loadNode(String className) {
        try {
            Class<?> clazz = classLoader.loadClass(className);
            Object instance = clazz.getDeclaredConstructor().newInstance();
            if (instance instanceof Node) return (Node) instance;
            throw new RuntimeException("Class " + className + " phải kế thừa Node");
        } catch (Throwable t) {
            System.err.println("Không load được node: " + className + " (" + t.getMessage() + ")");
            return null;
        }
    }

    /**
     * List tất cả class có trong jar
     */
    public List<String> findClasses() {
        List<String> classNames = new ArrayList<>();
        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".class")) {
                    String className = entry.getName()
                            .replace("/", ".")
                            .replace(".class", "");
                    classNames.add(className);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return classNames;
    }

    /**
     * Load overlay / config từ jar (JSON hoặc custom)
     */
    public OverlayConfig loadOverlay(String fileName) {
        try (JarFile jar = new JarFile(jarFile)) {
            JarEntry entry = jar.getJarEntry(fileName);
            if (entry != null) {
                try (InputStream in = jar.getInputStream(entry)) {
                    // parse JSON hoặc custom format
                    // ví dụ: return parseOverlayJson(in);
                    return new OverlayConfig();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // fallback mặc định
        return new OverlayConfig();
    }
}
