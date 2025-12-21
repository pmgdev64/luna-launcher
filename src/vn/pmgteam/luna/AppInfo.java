/*
 * Module: AppInfo.java
 * Chức năng: Lưu trữ thông tin cơ bản của launcher như phiên bản, tác giả, và tên ứng dụng.
 * Bản quyền: PmgTeam
 * Tác giả: PmgDev64
 * Ngày tạo: 04/11/2025
 */

package vn.pmgteam.luna;

public final class AppInfo {

    // Tên hiển thị của launcher
    public static final String APP_NAME = "LunaLauncher";

    // Phiên bản hiện tại của ứng dụng
    public static final String VERSION = "0.0.04";

    // Tác giả / nhà phát triển
    public static final String AUTHOR = "PmgTeam (PmgDev64)";

    // URL repository chính (GitHub)
    public static final String REPO_URL = "https://github.com/pmgdev64/luna-launcher";

    // URL API kiểm tra cập nhật (GitHub Release)
    public static final String UPDATE_API = "https://api.github.com/repos/pmgdev64/luna-launcher/releases/latest";

    // Mô tả hoặc banner (hiển thị trong About hoặc log)
    public static final String DESCRIPTION = 
        "LunaLauncher - Blue Archive themed launcher created by PmgTeam.";

    private AppInfo() {
        // Ngăn tạo instance
    }
}
