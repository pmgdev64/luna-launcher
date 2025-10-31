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
