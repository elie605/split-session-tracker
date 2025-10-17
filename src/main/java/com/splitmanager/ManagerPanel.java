package com.splitmanager;


import com.splitmanager.controllers.PanelController;
import com.splitmanager.views.PanelView;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.ui.PluginPanel;

/**
 * Composition root: builds Model, View, Controller and wires them together.
 * No UI logic or event handling lives here anymore.
 */
@Singleton
public class ManagerPanel extends PluginPanel
{

	private PanelController controller;
	private ManagerSession manager;
	private PluginConfig config;
	private ManagerKnownPlayers playerManager;

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

	public void start()
	{
		startPanel();
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
		PanelView view = new PanelView(manager, config, playerManager);
		controller = new PanelController(manager, config, view, playerManager, this);
		view.bindActions(controller);
		add(view);

		// initial sync
		controller.refreshAllView();
	}

	/**
	 * Recreate the panel components from scratch.
	 */
	public void restart()
	{
		removeAll();
		startPanel();
	}

	public void init()
	{
		startPanel();
	}
}
