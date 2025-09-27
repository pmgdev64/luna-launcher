package vn.pmgteam.kclient;

import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.*;

public class NofiticationBox {
	
	public String nofiticationTitle = "";
	public String nofiticationDesc = "";
	
	public NofiticationBox()
	{
		this.nofiticationTitle = nofiticationTitle;
		this.nofiticationDesc = nofiticationDesc;
	}

	public void showNofitication(String nofiticationTitle, String nofiticationDesc, float dur)
	{
		HBox nofiticationBox = new HBox(30);
		nofiticationBox.setAlignment(Pos.BOTTOM_RIGHT);
		nofiticationBox.setStyle("-fx-background-color: #ffffff");
		System.out.println("this is a nofiticationBox");
	}
}
