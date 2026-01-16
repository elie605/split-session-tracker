package com.splitmanager.views;

import com.splitmanager.ManagerKnownPlayers;
import com.splitmanager.ManagerSession;
import com.splitmanager.PluginConfig;
import com.splitmanager.controllers.PanelController;

public class PopoutView extends PanelView
{
	public PopoutView(ManagerSession sessionManager, PluginConfig config, ManagerKnownPlayers playerManager, PanelController controller)
	{
		super(sessionManager, config, playerManager, controller);
	}
}
