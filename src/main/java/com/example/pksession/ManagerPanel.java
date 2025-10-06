package com.example.pksession;

import com.example.pksession.views.PanelView;
import com.example.pksession.controllers.PanelController;
import com.sun.tools.jconsole.JConsoleContext;
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

    public ManagerPanel(ManagerSession manager, PluginConfig config) {
        this.manager = manager;
        this.config = config;
        startPanel();
    }

    public void refreshAllView() {
        controller.refreshAllView();
    }

    private void startPanel(){
        PanelView view = new PanelView(manager, config);
        controller = new PanelController(manager, config, view);
        view.bindActions(controller);
        add(view);

        // initial sync
        controller.refreshAllView();
    }

    public void restart(){
        removeAll();
        startPanel();
    }
}
