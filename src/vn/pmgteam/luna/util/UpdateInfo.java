package vn.pmgteam.luna.util;

public class UpdateInfo {
    private final String version;
    private final String changelog;
    private final String downloadUrl;
    private final boolean isPreRelease; // true nếu là bản beta/premium


    public UpdateInfo(String version, String changelog, String downloadUrl, boolean isPreRelease) {
        this.version = version;
        this.changelog = changelog;
        this.downloadUrl = downloadUrl;
		this.isPreRelease = isPreRelease;
    }

    public String getVersion() {
        return version;
    }

    public String getChangelog() {
        return changelog;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }
}
