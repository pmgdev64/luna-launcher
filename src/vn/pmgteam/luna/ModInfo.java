package vn.pmgteam.luna;

public class ModInfo {
    public String name;
    public String author;
    public String description;
    public int downloads;
    public String updated;
    public String version;

    @Override
    public String toString() {
        return name + " (" + version + ")";
    }
}
