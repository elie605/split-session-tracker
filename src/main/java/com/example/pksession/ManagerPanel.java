package com.example.pksession;

import com.example.pksession.views.PanelView;
import com.example.pksession.controllers.PanelController;
import net.runelite.client.ui.PluginPanel;

/**
 * Composition root: builds Model, View, Controller and wires them together.
 * No UI logic or event handling lives here anymore.
 */
public class ManagerPanel extends PluginPanel {

    private final PanelController controller;

    public ManagerPanel(ManagerSession manager, PluginConfig config) {
        PanelView view = new PanelView(manager, config);
        controller = new PanelController(manager, config, view);
        view.bindActions(controller);
        add(view);

        // initial sync
        controller.refreshAllView();
    }

    public void refreshAllView() {
        controller.refreshAllView();
    }
}
