package com.splitmanager;

import com.splitmanager.controllers.PanelController;
import com.splitmanager.views.PanelView;
import com.splitmanager.views.PopoutView;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.JButton;
import javax.swing.JPanel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import javax.swing.*;
import java.awt.*;


/**
 * Composition root: builds Model, View, Controller and wires them together.
 * No UI logic or event handling lives here anymore.
 */
@Slf4j
@Singleton
public class ManagerPanel
{

	private JFrame popoutFrame;
	private JButton popOutBtn;
	private PanelController controller;
	private final ManagerSession manager;
	private final PluginConfig config;
	private final ManagerKnownPlayers playerManager;
	@Getter
	@Setter
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
		controller = new PanelController(manager, config, playerManager, this);
		view = new PanelView(manager, config, playerManager,controller);
		controller.setView(view);

		if (config.enablePopout())
		{
			popOutBtn = new JButton("Pop Out");
			popOutBtn.addActionListener(e -> togglePopOutWindow());
			JPanel topBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
			topBar.add(popOutBtn);
			view.add(topBar, BorderLayout.NORTH,0);
		}

		controller.refreshAllView();
	}


	private void togglePopOutWindow()
	{
		if (popoutFrame != null && popoutFrame.isDisplayable())
		{
			return;
		}
		if (popOutBtn != null)//hide button if window open
		{
			popOutBtn.setVisible(false);
			view.revalidate();
			view.repaint();
		}

		PanelController ctrl = new PanelController(manager, config, playerManager, this);
		PopoutView popoutView = new PopoutView(manager, config, playerManager, ctrl);
		ctrl.setView(popoutView);

		ctrl.refreshAllView();

		popoutFrame = new JFrame("Auto Split Manager");
		popoutFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		JScrollPane scrollPane = new JScrollPane(popoutView);
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
					popoutView.revalidate();
					popoutView.repaint();
				}
			}
		});
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
