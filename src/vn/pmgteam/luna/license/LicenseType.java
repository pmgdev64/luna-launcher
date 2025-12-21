package vn.pmgteam.luna.license;

/**
 * LicenseType
 * Xác định quyền sử dụng tính năng
 */
public enum LicenseType {

    /**
     * Bản miễn phí
     * - Không cần token
     * - Không có Pro features
     */
    FREE,

    /**
     * Bản dùng thử
     * - Có thời hạn
     * - Giới hạn tính năng
     */
    TRIAL,

    /**
     * Bản trả phí (chuẩn)
     */
    PRO,

    /**
     * Bản Pro nhưng do admin cấp tay
     * (không thông qua payment)
     */
    PRO_MANUAL,

    /**
     * Bản nội bộ (dev / tester)
     */
    INTERNAL,

    /**
     * Bản không hợp lệ (fallback)
     */
    UNKNOWN
}
