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

	// log4j
    //requires org.apache.logging.log4j;
    //requires org.apache.logging.log4j.core;
    
	opens application to javafx.graphics, javafx.fxml;
}
