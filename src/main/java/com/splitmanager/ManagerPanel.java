package com.splitmanager;

import com.splitmanager.views.PanelView;
import com.splitmanager.controllers.PanelController;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.PluginPanel;
import javax.swing.*;
import java.awt.*;



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

    private JFrame popoutFrame;
    private JButton popOutBtn;

    /**
     * Initialize and wire the view and controller, and perform an initial sync.
     */
    private void startPanel()
    {
        setLayout(new BorderLayout());

        removeAll();

        PanelView view = new PanelView(manager, config);
        controller = new PanelController(manager, config, view);
        view.bindActions(controller);
        add(view, BorderLayout.CENTER);

        popOutBtn = new JButton("Pop Out");
        popOutBtn.addActionListener(e -> togglePopOutWindow());
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        topBar.add(popOutBtn);
        add(topBar, BorderLayout.NORTH);

        controller.refreshAllView();
        revalidate();
        repaint();
    }


    /**
     * Recreate the panel components from scratch.
     */
    public void restart() {
        removeAll();
        startPanel();
    }

    private void togglePopOutWindow()
    {
        if (popoutFrame != null && popoutFrame.isDisplayable()) {return;}
        if (popOutBtn != null)//hide button if window open
        {
            popOutBtn.setVisible(false);
            revalidate();
            repaint();
        }

        PanelView view = new PanelView(manager, config);
        PanelController ctrl = new PanelController(manager, config, view);
        view.bindActions(ctrl);
        ctrl.refreshAllView();

        popoutFrame = new JFrame("Auto Split Manager");
        popoutFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        JScrollPane scrollPane = new JScrollPane(view);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        popoutFrame.getContentPane().add(scrollPane, BorderLayout.CENTER);
        popoutFrame.pack();
        popoutFrame.setMinimumSize(new Dimension(800, 600));
        popoutFrame.setLocationRelativeTo(null);
        popoutFrame.setVisible(true);

        //show button if window closes
        popoutFrame.addWindowListener(new java.awt.event.WindowAdapter()
        {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e)
            {
                popoutFrame = null;
                if (popOutBtn != null)
                {
                    popOutBtn.setVisible(true);
                    revalidate();
                    repaint();
                }
            }
        });
    }





}
