package com.splitmanager;

import com.splitmanager.views.PanelView;
import com.splitmanager.controllers.PanelController;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.PluginPanel;

/**
 * Composition root: builds Model, View, Controller and wires them together.
 * No UI logic or event handling lives here anymore.
 */
@Slf4j
public class ManagerPanel extends PluginPanel {

    private PanelController controller;
    private ManagerSession manager;
    private PluginConfig config;

    /**
     * Construct a new plugin panel and bootstrap its MVC components.
     * @param manager session/state manager for split tracking
     * @param config plugin configuration
     */
    public ManagerPanel(ManagerSession manager, PluginConfig config) {
        this.manager = manager;
        this.config = config;
        startPanel();
    }

    /**
     * Refresh all view sections via the controller.
     */
    public void refreshAllView() {
        controller.refreshAllView();
    }

    /**
     * Initialize and wire the view and controller, and perform an initial sync.
     */
    private void startPanel() {
        PanelView view = new PanelView(manager, config);
        controller = new PanelController(manager, config, view);
        view.bindActions(controller);
        add(view);

        // initial sync
        controller.refreshAllView();
    }

    /**
     * Recreate the panel components from scratch.
     */
    public void restart() {
        removeAll();
        startPanel();
    }
}
