/*******************************************************************************
 * LunaLauncher 0.0.04
 * 
 * © 2025-2026 PmgDev64 & PmgTeam. Bảo lưu mọi quyền.
 * 
 * THÔNG TIN DỰ ÁN:
 *  - Tên dự án: LunaLauncher
 *  - Phiên bản: 0.0.04 (chủ đề earlyWinter)
 *  - Tác giả: PepperMCGamers Ch. PmgTeam (PmgDev64) & PmgTeam
 * 
 * THÔNG BÁO BẢN QUYỀN:
 *  1. Phần mềm và mã nguồn này được bảo vệ theo luật bản quyền quốc tế.
 *  2. Cấm sao chép, chỉnh sửa, phân phối hoặc sử dụng mã nguồn
 *     cho mục đích thương mại mà không có sự cho phép bằng văn bản từ tác giả.
 *  3. Mọi hành vi vi phạm có thể dẫn đến xử lý pháp lý.
 * 
 * LIÊN HỆ:
 *  - Email: tranhoang2009vqht@gmail.com
 *  - Website: https://pmgdev64.github.io/lunaLauncher
 * 
 * MIỄN TRỪ TRÁCH NHIỆM:
 *  - Phần mềm được cung cấp "nguyên trạng" không kèm bất kỳ bảo hành nào.
 *  - Sử dụng phần mềm là hoàn toàn tự chịu rủi ro. Tác giả không chịu trách
 *    nhiệm cho bất kỳ thiệt hại hay mất mát nào phát sinh.
 ******************************************************************************/
module launcher {
	requires javafx.controls;
	requires javafx.graphics;
	requires org.json;
	requires javafx.base;
	requires com.sun.jna;
	requires com.sun.jna.platform;
	requires javafx.swing;
	requires jdk.httpserver;
	requires java.net.http;
	requires com.fasterxml.jackson.databind;
	requires jopt.simple;
	requires com.luciad.imageio.webp;
	requires java.desktop;
	requires java.xml;
	requires java.base;
	requires jdk.internal.le;
	requires jdk.jlink;

	// log4j
    requires org.apache.logging.log4j;
 // Nếu bạn dùng cả phần core (ví dụ có file log4j2.xml, RollingFileAppender, v.v.)
    requires org.apache.logging.log4j.core;
    
    exports vn.pmgteam.luna;
    
    //requires org.apache.log4j;
    
    //requires org.apache.logging.log4j.to.slf4j;

    // Nếu bạn dùng SLF4J bridge (tùy chọn)

	opens application to javafx.graphics, javafx.fxml;
}
