package org.dockfx.model;

import org.dockfx.DockNode;

public class DockNodeProperty {

	private String settingName;
	private String title;

	public DockNodeProperty() {

	}

	public DockNodeProperty(DockNode dockNode) {
		this.settingName = dockNode.getSettingName();
		this.title = dockNode.getTitle();
	}

	public String getSettingName() {
		return settingName;
	}

	public void setSettingName(String settingName) {
		this.settingName = settingName;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

}
