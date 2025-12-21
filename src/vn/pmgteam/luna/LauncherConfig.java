package vn.pmgteam.luna;

import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.json.*;

import application.Main;

public class LauncherConfig {
    public String gamePath = Main.defaultGamePath;
    public boolean autoUpdate = true;
    public String theme = "dark";
    public String lastVersion = "1.21";
    public List<UserProfile> users = new ArrayList<>();
    public String language = "vi";

    public static List<String> lastCommandLine = new ArrayList<>();

    public LauncherConfig() {}
}
