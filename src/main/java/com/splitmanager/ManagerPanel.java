package com.splitmanager;


import com.splitmanager.controllers.PanelController;
import com.splitmanager.views.PanelView;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Composition root: builds Model, View, Controller and wires them together.
 * No UI logic or event handling lives here anymore.
 */
@Slf4j
@Singleton
public class ManagerPanel
{

	private PanelController controller;
	private final ManagerSession manager;
	private final PluginConfig config;
	private final ManagerKnownPlayers playerManager;
	@Getter
	private PanelView view;

	/**
	 * Construct a new plugin panel and bootstrap its MVC components.
	 *
	 * @param sessionManager session/state sessionManager for split tracking
	 * @param config         plugin configuration
	 */
	@Inject
	public ManagerPanel(ManagerSession sessionManager, PluginConfig config, ManagerKnownPlayers playerManager)
	{
		this.manager = sessionManager;
		this.config = config;
		this.playerManager = playerManager;
	}

	/**
	 * Refresh all view sections via the controller.
	 */
	public void refreshAllView()
	{
		controller.refreshAllView();
	}

	/**
	 * Initialize and wire the view and controller, and perform an initial sync.
	 */
	private void startPanel()
	{
		view = new PanelView(manager, config, playerManager);
		controller = new PanelController(manager, config, view, playerManager, this);
		view.bindActions(controller);
		controller.refreshAllView();
	}

	/**
	 * Recreate the panel components from scratch.
	 */
	public void restart()
	{
		view.removeAll();
		startPanel();
	}

	public void init()
	{
		startPanel();
	}
}
