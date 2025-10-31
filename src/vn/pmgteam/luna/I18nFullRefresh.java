package vn.pmgteam.luna;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.Tooltip;

import java.util.List;

/**
 * Helper I18n: refresh toàn bộ UI khi đổi ngôn ngữ
 * 
 * Sử dụng:
 * 1. Gắn userData = "i18n.key" cho các node cần i18n:
 *      Label lbl = new Label(I18n.get("settings.theme"));
 *      lbl.setUserData("settings.theme");
 * 2. Khi đổi ngôn ngữ:
 *      I18n.setLocale(getClass(), newLocale);
 *      I18nFullRefresh.refreshAll(root, tabPane, menuBar);
 */
public class I18nFullRefresh {

    /** Refresh tất cả node tree */
	public static void refreshNode(Node node) {
	    // Labeled: Label, Button, CheckBox, RadioButton...
	    if (node instanceof Labeled labeled && labeled.getUserData() instanceof String key) {
	        labeled.setText(I18n.get(key));
	    }

	    // Control: tooltip
	    if (node instanceof Control control) {
	        Tooltip tip = control.getTooltip();
	        if (tip != null && tip.getUserData() instanceof String tipKey) {
	            tip.setText(I18n.get(tipKey));
	        }
	    }

	    // Duyệt children nếu là Parent
	    if (node instanceof Parent parent) {
	        for (Node child : parent.getChildrenUnmodifiable()) {
	            refreshNode(child);
	        }
	    }
	}


    /** Refresh tất cả Tab */
    public static void refreshTabs(TabPane tabPane) {
        for (Tab tab : tabPane.getTabs()) {
            if (tab.getUserData() instanceof String key) {
                tab.setText(I18n.get((String) tab.getUserData()));
            }
            if (tab.getContent() != null) {
                refreshNode(tab.getContent());
            }
        }
    }

    /** Refresh tất cả Menu/MenuItem */
    public static void refreshMenu(MenuBar menuBar) {
        for (Menu menu : menuBar.getMenus()) {
            if (menu.getUserData() instanceof String key) {
                menu.setText(I18n.get((String) menu.getUserData()));
            }
            refreshMenuItems(menu.getItems());
        }
    }

    private static void refreshMenuItems(List<MenuItem> items) {
        for (MenuItem item : items) {
            if (item.getUserData() instanceof String key) {
                item.setText(I18n.get((String) item.getUserData()));
            }
            if (item instanceof Menu menu) {
                refreshMenuItems(menu.getItems()); // recursive for submenu
            }
        }
    }

    /** Refresh ComboBox items từ danh sách i18n key */
    public static void updateComboBoxItems(ComboBox<String> combo, String... keys) {
        combo.getItems().clear();
        for (String key : keys) {
            combo.getItems().add(I18n.get(key));
        }
    }

    /** Refresh TabPane titles từ danh sách i18n key (theo thứ tự tab) */
    public static void updateTabTitles(TabPane tabPane, String... keys) {
        for (int i = 0; i < tabPane.getTabs().size() && i < keys.length; i++) {
            Tab tab = tabPane.getTabs().get(i);
            tab.setText(I18n.get(keys[i]));
        }
    }

    /** Refresh toàn bộ UI */
    public static void refreshAll(Node root, TabPane tabPane, MenuBar menuBar) {
        refreshNode(root);
        if (tabPane != null) refreshTabs(tabPane);
        if (menuBar != null) refreshMenu(menuBar);
    }
}
