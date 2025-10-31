package vn.pmgteam.luna;

import javafx.scene.control.Label;

public class I18nLabel extends Label {
    private final String key;

    public I18nLabel(String key) {
        super(I18n.get(key));
        this.key = key;

        I18n.addListener(() -> setText(I18n.get(key)));
    }
}
