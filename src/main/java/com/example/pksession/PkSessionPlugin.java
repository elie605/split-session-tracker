package com.example.pksession;

import com.google.inject.Provides;

import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import java.awt.image.BufferedImage;

@Slf4j
@PluginDescriptor(
	name = "PK Session",
	description = "Track PK sessions, roster changes, kills and live splits. Supports history.",
	enabledByDefault = true
)
public class PkSessionPlugin extends Plugin
{
	@Inject private Client client;
	@Inject private ClientToolbar clientToolbar;
	@Inject private PkSessionConfig config;

	private com.example.pksession.PkSessionPanel panel;
	private NavigationButton navButton;
	private com.example.pksession.SessionManager sessionManager;

    @Override
    protected void startUp()
    {
        sessionManager = new com.example.pksession.SessionManager(config);
        sessionManager.loadFromConfig(); // load sessions and players (peeps)

        panel = new com.example.pksession.PkSessionPanel(sessionManager, config, this::requestUiRefresh);

        // TODO create an icon
        // Use a transparent placeholder icon so the panel shows in the side menu without bundling an image.
        BufferedImage placeholderIcon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);

        navButton = NavigationButton.builder()
                .tooltip("PK Session")
                .icon(placeholderIcon)
                .priority(5)
                .panel(panel)
                .build();
        clientToolbar.addNavigation(navButton);
    }


	@Override
	protected void shutDown()
	{
		if (navButton != null)
		{
			clientToolbar.removeNavigation(navButton);
			navButton = null;
		}
		if (sessionManager != null)
		{
			sessionManager.saveToConfig();
		}
		panel = null;
	}

	@Provides
	PkSessionConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PkSessionConfig.class);
	}

	private void requestUiRefresh()
	{
		SwingUtilities.invokeLater(() -> {
			if (panel != null)
			{
				panel.refresh();
			}
		});
	}
}
