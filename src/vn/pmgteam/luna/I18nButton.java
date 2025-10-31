package vn.pmgteam.luna;

import javafx.scene.control.Button;

public class I18nButton extends Button {
    private final String key;

    public I18nButton(String key) {
        super(I18n.get(key));
        this.key = key;

        I18n.addListener(() -> setText(I18n.get(key)));
    }
}
