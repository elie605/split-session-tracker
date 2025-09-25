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
import java.text.ParseException;

@Slf4j
@PluginDescriptor(
	name = "PK Session",
	description = "Automatic loot splitter for PK sessions. Tracks sessions, roster changes, chat-detected values, editable recent splits, and settlement metrics (copy JSON). Collapsible sections and configurable panel order.",
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
        panel.refreshAllView();

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
				panel.refreshAllView();
			}
		});
	}

	@Subscribe
	public void onChatMessage(ChatMessage event) throws ParseException {
        Formats.OsrsAmountFormatter f = new Formats.OsrsAmountFormatter();

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
                Long value = (Long) f.stringToValue(m.group(2));
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
                Long value = (Long) f.stringToValue(m.group(3));
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
				Long valueK = (Long) f.stringToValue(amtText);
				queuePending(com.example.pksession.model.PendingValue.Type.ADD, isClan ? "Clan" : "Friends", msg, valueK, who);
			}
		}
	}

	private Long parseNumber(String s)
	{
		if (s == null) return 0L;
		try { return Long.parseLong(s.replace(",", "")); } catch (Exception e) { return 0L; }
	}

	private void queuePending(com.example.pksession.model.PendingValue.Type type, String source, String msg, Long value, String suggestedPlayer)
	{
		if (sessionManager == null) return;
		com.example.pksession.model.PendingValue pv = com.example.pksession.model.PendingValue.of(type, source, msg, value, suggestedPlayer);
		sessionManager.addPendingValue(pv);
		// Ask UI to refresh
		requestUiRefresh();
	}
}
