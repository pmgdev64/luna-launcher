package vn.pmgteam.luna.license;

/**
 * LicenseStatus
 * Trạng thái của License do Server trả về
 */
public enum LicenseStatus {

    /**
     * License hợp lệ và đã được kích hoạt
     */
    ACTIVATED,

    /**
     * License hợp lệ nhưng chưa kích hoạt
     * (VD: vừa mua, chờ bind HWID)
     */
    PENDING,

    /**
     * License không tồn tại / token sai định dạng
     */
    INVALID,

    /**
     * License đã hết hạn
     */
    EXPIRED,

    /**
     * License bị thu hồi (crack / leak / vi phạm)
     */
    UNAVAILABLE,

    /**
     * Server không thể kiểm tra (offline, timeout)
     */
    SERVER_UNREACHABLE,

    /**
     * License không còn được hỗ trợ bởi phiên bản launcher hiện tại
     */
    UNSUPPORTED_VERSION,

    /**
     * License hợp lệ nhưng không khớp HWID
     */
    HWID_MISMATCH,

    /**
     * License bị khóa tạm thời (spam request, nghi ngờ gian lận)
     */
    SUSPENDED,

    /**
     * License hợp lệ nhưng đang ở chế độ Trial
     */
    TRIAL,

    /**
     * Trạng thái không xác định (fallback)
     */
    UNKNOWN
}
