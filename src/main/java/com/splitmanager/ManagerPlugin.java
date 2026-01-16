package com.splitmanager;

import com.google.inject.Provides;
import com.splitmanager.models.PendingValue;
import com.splitmanager.models.Session;
import com.splitmanager.utils.ChatStatusOverlay;
import com.splitmanager.utils.Formats;
import com.splitmanager.views.PanelView;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.text.ParseException;
import java.util.Arrays;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.Player;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanChannelMember;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ClanChannelChanged;
import net.runelite.api.events.FriendsChatChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.WorldChanged;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.WidgetUtil;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(
	name = "Auto Split Manager",
	description = "Automatically track and manage splits for group PvM/PvP. Features include: chat detection of drops, player roster management with alt support, split calculations, session tracking, settlement metrics, and configurable UI. JSON export available for sharing and backup.",
	tags = {"splits", "loot", "pvm", "pvp", "tracker", "clan", "group"}
)
/**
 * Main RuneLite plugin entry point for Auto Split Manager.
 * Wires up UI, session management, configuration, and chat/menu event handlers.
 */
public class ManagerPlugin extends Plugin
{
	@Getter
	private static ManagerPanel panel;
	@Inject
	private Client client;
	@Inject
	private ClientToolbar clientToolbar;
	@Getter
	@Inject
	private PluginConfig config;
	@Inject
	private OverlayManager overlayManager;
	private ChatStatusOverlay chatOverlay;
	private NavigationButton navButton;

	@Inject
	private ManagerPanel panelManager;
	@Inject
	private ManagerSession sessionManager;
	@Inject
	private ManagerKnownPlayers playerManager;
	private PanelView view;


	@Override
	/**
	 * Initialize plugin state and register the sidebar panel/navigation.
	 */
	protected void startUp()
	{
		Formats.setConfig(config);
		playerManager.init();
		sessionManager.init();
		panelManager.init();

		// TODO create an icon

		chatOverlay = new ChatStatusOverlay();
		overlayManager.add(chatOverlay);
		view = panelManager.getView();
		navButton = NavigationButton.builder()
			.tooltip("Auto Split Manager")
			.icon(createIcon())
			.priority(5)
			.panel(view)
			.build();
		clientToolbar.addNavigation(navButton);
	}


	private BufferedImage createIcon(){
		BufferedImage scalesIcon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		java.awt.Graphics2D g = scalesIcon.createGraphics();
		g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

		// Define colors
		java.awt.Color darkBrown = new java.awt.Color(58, 40, 28);
		java.awt.Color orange = new java.awt.Color(246, 135, 79);
		java.awt.Color cream = new java.awt.Color(255, 224, 159);
		java.awt.Color black = new java.awt.Color(0, 0, 0);

		// First draw black outlines/shadows (slightly offset)
		g.setColor(black);
		g.setStroke(new java.awt.BasicStroke(2.0f));

		// Shadow for the center vertical line - made taller
		g.drawLine(8, 1, 8, 12);

		// Shadow for the horizontal balance bar - moved up slightly
		g.drawLine(2, 3, 14, 3);

		// Shadow for triangular connections - adjusted for taller appearance
		g.drawLine(4, 3, 1, 8); // Left triangle
		g.drawLine(12, 3, 15, 8); // Right triangle

		// Shadow for the left scale bowl - moved down slightly
		g.drawArc(0, 8, 6, 4, 0, 180);

		// Shadow for the right scale bowl - moved down slightly
		g.drawArc(10, 8, 6, 4, 0, 180);

		// Shadow for the base - moved down
		g.drawArc(5, 12, 6, 4, 0, 180);

		// Now draw the actual elements

		// Draw the center vertical line (pole) - made taller
		g.setColor(darkBrown);
		g.setStroke(new java.awt.BasicStroke(1.5f));
		g.drawLine(8, 1, 8, 12);

		// Draw the horizontal balance bar - moved up slightly
		g.drawLine(2, 3, 14, 3);

		// Draw the triangular connections - adjusted for taller appearance
		g.drawLine(4, 3, 1, 8); // Left triangle
		g.drawLine(12, 3, 15, 8); // Right triangle

		// Draw the left scale bowl outline - moved down slightly
		g.drawArc(0, 8, 6, 4, 0, 180);

		// Draw the right scale bowl outline - moved down slightly
		g.drawArc(10, 8, 6, 4, 0, 180);

		// Fill the bowls with orange
		g.setColor(orange);
		g.fillArc(1, 9, 4, 2, 0, 180);
		g.fillArc(11, 9, 4, 2, 0, 180);

		// Draw the base outline - moved down
		g.setColor(darkBrown);
		g.drawArc(5, 12, 6, 4, 0, 180);

		// Fill the base with cream color
		g.setColor(cream);
		g.fillArc(6, 13, 4, 2, 0, 180);

		g.dispose();

		return scalesIcon;
	}

	@Override
	/**
	 * Persist state and remove UI elements when the plugin shuts down.
	 */
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

		if (chatOverlay != null)
		{
			overlayManager.remove(chatOverlay);
			chatOverlay = null;
		}
	}

	/**
	 * Provide injectable configuration instance.
	 *
	 * @param configManager RuneLite config manager
	 * @return plugin config
	 */
	@Provides
	PluginConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PluginConfig.class);
	}

	@Subscribe
	public void onClanChannelChanged(ClanChannelChanged e)
	{
		updateChatWarningStatus();
	}

	@Subscribe
	public void onFriendsChatChanged(FriendsChatChanged e)
	{
		updateChatWarningStatus();
	}

	@Subscribe
	public void onWorldChanged(WorldChanged e)
	{
		updateChatWarningStatus();
	}

	/**
	 * React to plugin configuration changes that require a panel refresh/restart.
	 *
	 * @param e config change event
	 */
	@Subscribe
	public void onConfigChanged(ConfigChanged e)
	{
		if (!"Split Manager".equals(e.getGroup()))
		{
			return;
		}

		switch (e.getKey())
		{
			case "directPayments":
				log.info("Direct payments changed, refreshing panel");
				restartViewFix();
				break;
			case "WarnNotInFC":
				updateChatWarningStatus();
				break;
			case "enablePopout":
			case "enableTour":
				restartViewFix();
				break;
		}
	}

	private void restartViewFix()
	{
		panelManager.restart();
		view = panelManager.getView();
		if (navButton != null)
		{
			clientToolbar.removeNavigation(navButton);
			navButton = NavigationButton.builder()
				.tooltip("Auto Split Manager")
				.icon(navButton.getIcon())
				.priority(5)
				.panel(view)
				.build();
			clientToolbar.addNavigation(navButton);
		}
	}

	/**
	 * Parse chat messages to detect values and enqueue PendingValue suggestions.
	 *
	 * @param event chat message event
	 * @throws ParseException when number parsing fails
	 */
	@Subscribe
	public void onChatMessage(ChatMessage event) throws ParseException
	{

		// Disabled for now, this should be covered by other checks
		if (CheckChatJoinLeave(event))
		{
			return;
		}

		if (!config.enableChatDetection())
		{
			return;
		}

		Formats.OsrsAmountFormatter f = new Formats.OsrsAmountFormatter();
		ChatMessageType type = event.getType();
		String tname = type.name();

		boolean isClan = tname.contains("CLAN");
		boolean isFriends = tname.contains("FRIEND");

		if (isClan && !config.detectInClanChat())
		{
			return;
		}
		if (isFriends && !config.detectInFriendsChat())
		{
			return;
		}
		if (!isClan && !isFriends)
		{
			return;
		}

		String msg = event.getMessage();

		// Try parse PvM drop
		if (config.detectPvmValues())
		{
			java.util.regex.Matcher m = java.util.regex.Pattern.compile("^(.+?) has received a drop: .*?\\((\\d[\\d,]*) coins\\)").matcher(msg);
			if (m.find())
			{
				String player = m.group(1);
				Long value = (Long) f.stringToValue(m.group(2) + " coins");
				queuePending(PendingValue.Type.PVM, isClan ? "Clan" : "Friends", msg, value, player);
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
				Long value = (Long) f.stringToValue(m.group(3) + " coins");
				queuePending(PendingValue.Type.PVP, isClan ? "Clan" : "Friends", msg, value, player);
				return;
			}
		}

		// Try parse player !add value
		//TODO fix negative numbers
		if (config.detectPlayerValues())
		{
			// Pattern to match a single value with k, m, or b unit only
			String valuePattern = "([0-9][0-9,]*(?:\\.[0-9]+)?)\\s*([kmb])?";

			// Pattern to match one or more values separated by spaces or commas
			java.util.regex.Pattern multiValuePattern = java.util.regex.Pattern
				.compile("(?i)!add\\s+(" + valuePattern + "(\\s*,?\\s*" + valuePattern + ")*)");

			java.util.regex.Matcher multiMatcher = multiValuePattern.matcher(msg);

			if (multiMatcher.find())
			{
				String sender = event.getName();
				String who = sender.replaceAll("<[^>]*>", "");
				String valuesText = multiMatcher.group(1);
				String[] valueStrings = valuesText.split("\\s*,\\s*|\\s+");

				for (String valueString : valueStrings)
				{
					java.util.regex.Matcher singleValueMatcher = java.util.regex.Pattern
						.compile("(?i)(" + valuePattern + ")")
						.matcher(valueString);

					if (singleValueMatcher.find())
					{
						String numberTxt = singleValueMatcher.group(2);
						String unitTxt = singleValueMatcher.group(3);
						if (unitTxt == null)
						{
							unitTxt = config.defaultValueMultiplier().getValue();
						}

						// Combine the number and unit for parsing
						String fullValueText = numberTxt + (unitTxt != null ? unitTxt : "");

						try
						{
							Long value = (Long) f.stringToValue(fullValueText);
							queuePending(PendingValue.Type.ADD, isClan ? "Clan" : "Friends",
								"!add " + fullValueText, value, who);
						}
						catch (ParseException e)
						{
							// Skip invalid values
							log.debug("Failed to parse value: " + fullValueText, e);
						}
					}
				}
			}
		}
	}


	/**
	 * Enqueue a pending value suggestion for user approval.
	 *
	 * @param type            source type (PvM, PvP, player add)
	 * @param source          chat source label
	 * @param msg             original chat message
	 * @param value           numeric value (coins or K)
	 * @param suggestedPlayer prefilled player name when available
	 */
	private void queuePending(PendingValue.Type type, String source, String msg, Long value, String suggestedPlayer)
	{
		if (sessionManager == null)
		{
			return;
		}
		PendingValue pv = PendingValue.of(type, source, msg, value, suggestedPlayer);
		sessionManager.addPendingValue(pv);

		panelManager.refreshAllView();
	}

	private boolean CheckChatJoinLeave(ChatMessage event)
	{

		String plain = Text.removeTags(event.getMessage()).trim();
		String lower = plain.toLowerCase();

		ChatMessageType t = event.getType();
		boolean isSystemish = t == ChatMessageType.GAMEMESSAGE
			|| t == ChatMessageType.CLAN_MESSAGE
			|| t == ChatMessageType.CLAN_CHAT
			|| t == ChatMessageType.CLAN_GUEST_CHAT
			|| t.name().contains("CLAN");

		if (!isSystemish)
		{
			return false;
		}

		//LEAVE/KICK Chat
		if (java.util.regex.Pattern
			.compile("(?i)^\\s*(?:you\\s+(?:have\\s+)?left\\s+(?:the\\s+)?(?:chat-)?channel\\.?|you\\s+(?:are|aren't|are\\s+not)\\s+currently\\s+in\\s+(?:a|the|your)\\s+(?:chat-)?channel\\.?|you\\s+have\\s+been\\s+kicked\\s+from\\s+the\\s+channel\\.?)\\s*$")
			.matcher(plain)
			.find())
		{
			updateChatWarningStatus();
			return true;
		}


		//JOIN Chat
		if (java.util.regex.Pattern
			.compile("(?i)^\\s*now\\s+talking\\s+in\\s+(?:the\\s+)?(?:chat-)?channel\\.?\\s*$")
			.matcher(plain)
			.find())
		{
			updateChatWarningStatus();
			return false;
		}
		return false;
	}


	/**
	 * Recompute overlay purely from member lists (no timers, no message heuristics).
	 */
	public void updateChatWarningStatus()
	{
		if (chatOverlay == null)
		{
			return;
		}

		if (sessionManager.getCurrentSession().isEmpty() || !config.warnNotFC())
		{
			chatOverlay.setVisible(false);
			return;
		}

		log.info("Updating chat overlay status");
		log.info("{}", isFriendsChatOn());

		if (isFriendsChatOn())
		{
			chatOverlay.setVisible(false);
			chatOverlay.setChatchanOn(true);
			return;
		}

		chatOverlay.setVisible(true);
		chatOverlay.setChatchanOn(false);
	}


	/**
	 * Track context (in game) menu openings to add an option to add/remove players from session.
	 * This Triggers when you right click a player in the friends/clan chat.
	 *
	 * @param event menu entry added event
	 */
	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		int componentId = event.getActionParam1();
		int groupId = WidgetUtil.componentToInterface(componentId);

		Session currentSession = null;
		if (sessionManager == null)
		{
			return;
		}
		else
		{
			currentSession = sessionManager.getCurrentSession().orElse(null);
		}
		if (!(groupId == InterfaceID.FRIENDS || groupId == InterfaceID.CHATCHANNEL_CURRENT
			|| componentId == InterfaceID.ClansSidepanel.PLAYERLIST || componentId == InterfaceID.ClansGuestSidepanel.PLAYERLIST))
		{
			return;
		}
		String playername = Text.removeTags(event.getTarget());

		if (currentSession == null)
		{
			String removeFromSession = "Add to known players";

			if (playerManager.isKnownPlayer(playername))
			{
				return;
			}
			// TODO Fix bug: For some reason this event/function triggers twice, so i have to check that the entry doesn't already exist' and i feel like i should not have to check this.
			// This might be a janky mess but idc
			if (Arrays.stream(client.getMenu().getMenuEntries()).anyMatch(e -> e.getOption().equals(removeFromSession)))
			{
				return;
			}

			client.getMenu().createMenuEntry(-1)
				.setOption(removeFromSession)
				.setTarget(event.getTarget())
				.setType(MenuAction.RUNELITE)
				.onClick(e ->
				{
					playerManager.addKnownPlayer(playername);
					panelManager.refreshAllView();
				});
			return;
		}


		if (sessionManager.currentSessionHasPlayer(playername))
		{
			String removeFromSession = "Remove from session";

			// TODO Fix bug: For some reason this event/function triggers twice, so i have to check that the entry doesn't already exist' and i feel like i should not have to check this.
			// This might be a janky mess but idc
			if (Arrays.stream(client.getMenu().getMenuEntries()).anyMatch(e -> e.getOption().equals(removeFromSession)))
			{
				return;
			}

			client.getMenu().createMenuEntry(-1)
				.setOption(removeFromSession)
				.setTarget(event.getTarget())
				.setType(MenuAction.RUNELITE)
				.onClick(e ->
				{
					sessionManager.removePlayerFromSession(playername);
					panelManager.refreshAllView();
				});
			return;
		}

		String removeFromSession = "Add to session";

		// TODO Fix bug: For some reason this event/function triggers twice, so i have to check that the entry doesn't already exist' and i feel like i should not have to check this.
		// This might be a janky mess but idc
		if (Arrays.stream(client.getMenu().getMenuEntries()).anyMatch(e -> e.getOption().equals(removeFromSession)))
		{
			return;
		}

		client.getMenu().createMenuEntry(-1)
			.setOption(removeFromSession)
			.setTarget(event.getTarget())
			.setType(MenuAction.RUNELITE)
			.onClick(e ->
			{
				if (playerManager.isKnownPlayer(playername, true))
				{
					sessionManager.addPlayerToActive(playername);
				}
				panelManager.refreshAllView();
			});
	}


	/**
	 * Local player's cleaned display name ("" if not ready).
	 */
	private String myCleanName()
	{
		Player me = client.getLocalPlayer();
		if (me == null)
		{
			return "";
		}
		return net.runelite.client.util.Text.toJagexName(
			net.runelite.client.util.Text.removeTags(me.getName()));
	}

	/**
	 * True iff the given clan channel currently contains *you*.
	 */
	private boolean channelHasSelf(ClanChannel ch)
	{
		if (ch == null || ch.getMembers() == null)
		{
			return false;
		}
		String me = myCleanName();
		if (me.isEmpty())
		{
			return false;
		}

		for (ClanChannelMember m : ch.getMembers())
		{
			if (m == null)
			{
				continue;
			}
			String n = m.getName();
			if (n == null)
			{
				continue;
			}
			n = net.runelite.client.util.Text.toJagexName(
				net.runelite.client.util.Text.removeTags(n));
			if (me.equalsIgnoreCase(n))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Joined to main / guest chat-channel (based solely on your presence).
	 */
	private boolean isMainClanChatOn()
	{
		return channelHasSelf(client.getClanChannel());
	}

	private boolean isGuestClanChatOn()
	{
		return channelHasSelf(client.getGuestClanChannel());
	}

	/**
	 * Joined to Friends Chat ("Chat Channel")?
	 */
	private boolean isFriendsChatOn()
	{
		return client.getFriendsChatManager() != null;
	}
}
