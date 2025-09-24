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
import net.runelite.api.events.ChatMessage;
import net.runelite.api.ChatMessageType;
import net.runelite.client.eventbus.Subscribe;

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

	private static com.example.pksession.PkSessionPanel panel;
	private NavigationButton navButton;
	private com.example.pksession.SessionManager sessionManager;

    public static PkSessionPanel getPanel(){
        if ( panel == null)
            throw new IllegalStateException("Panel not initialized");
        return panel;
    }

    @Override
    protected void startUp()
    {
        sessionManager = new com.example.pksession.SessionManager(config);
        sessionManager.loadFromConfig(); // load sessions and players (peeps)

        panel = new com.example.pksession.PkSessionPanel(sessionManager, config);
        panel.refresh();

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

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (!config.enableChatDetection())
		{
			return;
		}
		ChatMessageType type = event.getType();
		String tname = type.name();
		boolean isClan = tname.contains("CLAN");
		boolean isFriends = tname.contains("FRIEND");
		if (isClan && !config.detectInClanChat()) return;
		if (isFriends && !config.detectInFriendsChat()) return;
		if (!isClan && !isFriends) return; // only these channels

		String msg = event.getMessage();
		String sender = event.getName(); // may include rank/icon tags
		if (sender != null) sender = sender.replaceAll("<[^>]*>", "");

		// Try parse PvM drop
		if (config.detectPvmValues())
		{
			java.util.regex.Matcher m = java.util.regex.Pattern.compile("^(.+?) has received a drop: .*?\\((\\d[\\d,]*) coins\\)").matcher(msg);
			if (m.find())
			{
				String player = m.group(1);
				double value = parseNumber(m.group(2));
				queuePending(com.example.pksession.model.PendingValue.Type.PVM, isClan ? "Clan" : "Friends", msg, value, player);
				return;
			}
		}

		// Try parse PvP loot
		if (config.detectPvpValues())
		{
			java.util.regex.Matcher m = java.util.regex.Pattern.compile("^(.+?) has defeated (.+?) and received \\((\\d[\\d,]*) coins\\) worth of loot!").matcher(msg);
			if (m.find())
			{
				String player = m.group(1);
				double value = parseNumber(m.group(3));
				queuePending(com.example.pksession.model.PendingValue.Type.PVP, isClan ? "Clan" : "Friends", msg, value, player);
				return;
			}
		}

		// Try parse player !add value
		if (config.detectPlayerValues())
		{
			// Accept forms like: !add 250k, !add 1.2m, !add 3b, or plain numbers (treated as K by formatter)
			java.util.regex.Matcher m = java.util.regex.Pattern
					.compile("(?i)!add\\s+([0-9][0-9,]*(?:\\.[0-9]+)?\\s*[kmb]?)")
					.matcher(msg);
			if (m.find())
			{
				String who = sender != null ? sender : "";
				String amtText = m.group(1);
				double valueK = parseKAmount(amtText);
				queuePending(com.example.pksession.model.PendingValue.Type.ADD, isClan ? "Clan" : "Friends", msg, valueK, who);
			}
		}
	}

	private double parseNumber(String s)
	{
		if (s == null) return 0;
		try { return Double.parseDouble(s.replace(",", "")); } catch (Exception e) { return 0; }
	}

	// Parse amounts like 250k, 1.2m, 3b into K units using the same logic as the UI formatter
	private double parseKAmount(String text)
	{
		try {
			Formats.OsrsAmountFormatter f = new Formats.OsrsAmountFormatter();
			Object v = f.stringToValue(text);
			if (v instanceof Number) {
				return ((Number) v).doubleValue();
			}
		} catch (java.text.ParseException ignored) { }
		// Fallback: plain number treated as coins -> convert to K
		double coins = parseNumber(text);
		return coins / 1000.0;
	}

	private void queuePending(com.example.pksession.model.PendingValue.Type type, String source, String msg, double value, String suggestedPlayer)
	{
		if (sessionManager == null) return;
		com.example.pksession.model.PendingValue pv = com.example.pksession.model.PendingValue.of(type, source, msg, value, suggestedPlayer);
		sessionManager.addPendingValue(pv);
		// Ask UI to refresh
		requestUiRefresh();
	}
}
