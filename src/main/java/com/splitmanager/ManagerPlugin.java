package com.splitmanager;

import com.splitmanager.models.Session;
import com.splitmanager.utils.Formats;
import com.splitmanager.utils.Utils;
import com.google.inject.Provides;

import javax.inject.Inject;

import com.splitmanager.models.PendingValue;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanChannelMember;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.WidgetUtil;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.util.Text;
import net.runelite.api.events.ClanChannelChanged;
import net.runelite.api.events.FriendsChatChanged;
import net.runelite.api.events.WorldChanged;
import net.runelite.api.events.GameTick;

import java.awt.*;

import java.awt.image.BufferedImage;
import java.text.ParseException;
import java.util.Arrays;

import net.runelite.api.FriendsChatManager;
import net.runelite.api.FriendsChatMember;
import net.runelite.client.ui.overlay.OverlayManager;
import com.splitmanager.utils.ChatStatusOverlay;

@Slf4j
@PluginDescriptor(
        name = "Auto Split Manager",
        description = "Automatic split manager for group sessions. Tracks sessions, roster changes, chat-detected values, editable recent splits, and settlement metrics (copy JSON). Collapsible sections and configurable panel order.",
        enabledByDefault = true
)
/**
 * Main RuneLite plugin entry point for Auto Split Manager.
 * Wires up UI, session management, configuration, and chat/menu event handlers.
 */
public class ManagerPlugin extends Plugin {
    @Inject
    private Client client;
    @Inject
    private ClientToolbar clientToolbar;
    @Inject
    private PluginConfig config;

    //SIGH
    @Inject
    private OverlayManager overlayManager;
    private ChatStatusOverlay chatOverlay;
    private boolean chatExplicitKnown = false;
    private boolean chatExplicitOn = false;

    @Getter
    private static ManagerPanel panel;
    private NavigationButton navButton;

    @Inject
    private ManagerPanel panelManager;
    @Inject
    private ManagerSession sessionManager;
    @Inject
    private ManagerKnownPlayers playerManager;


    @Override
    /**
     * Initialize plugin state and register the sidebar panel/navigation.
     */
    protected void startUp() {
        playerManager.init();
        sessionManager.init();
        panelManager.init();

        panelManager.refreshAllView();

        // TODO create an icon
        // Use a transparent placeholder icon so the panel shows in the side menu without bundling an image.
        BufferedImage placeholderIcon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);

        chatOverlay = new ChatStatusOverlay(config);
        overlayManager.add(chatOverlay);
        updateLootChatStatus();
        navButton = NavigationButton.builder()
                .tooltip("Auto Split Manager")
                .icon(placeholderIcon)
                .priority(5)
                .panel(panelManager)
                .build();
        clientToolbar.addNavigation(navButton);
    }


    @Override
    /**
     * Persist state and remove UI elements when the plugin shuts down.
     */
    protected void shutDown() {
        if (navButton != null) {
            clientToolbar.removeNavigation(navButton);
            navButton = null;
        }
        if (sessionManager != null) {
            //TODO It might be smart to save data during runtime.
            sessionManager.saveToConfig();
        }

        //SIGH
        if (chatOverlay != null) {
            overlayManager.remove(chatOverlay);
            chatOverlay = null;
        }

        panelManager = null;
    }

    @Provides
    /**
     * Provide injectable configuration instance.
     * @param configManager RuneLite config manager
     * @return plugin config
     */
    PluginConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(PluginConfig.class);
    }

    //SIGH
    @Subscribe
    public void onClanChannelChanged(ClanChannelChanged e) {
        updateLootChatStatus();
    }

    @Subscribe
    public void onFriendsChatChanged(FriendsChatChanged e) {
        updateLootChatStatus();
    }

    @Subscribe
    public void onWorldChanged(WorldChanged e) {
        updateLootChatStatus();
    }

    @Subscribe
    public void onGameTick(GameTick t) {
        updateLootChatStatus();
    }

    @Subscribe
    public void onGameStateChanged(net.runelite.api.events.GameStateChanged e) {
        switch (e.getGameState()) {
            case LOGGING_IN:
            case HOPPING:
            case LOADING:
                chatExplicitKnown = false;
                break;
            default:
                break;
        }
        updateLootChatStatus();
    }


    @Subscribe
    /**
     * React to plugin configuration changes that require a panel refresh/restart.
     * @param e config change event
     */
    public void onConfigChanged(ConfigChanged e) {
        if ("Split Manager".equals(e.getGroup()) && "directPayments".equals(e.getKey())) {
            log.info("Direct payments changed, refreshing panel");
            panelManager.restart();
        }

        //SIGH
        if ("Split Manager".equals(e.getGroup())) {
            switch (e.getKey()) {
                case "enableChatDetection":
                case "detectInClanChat":
                case "detectInFriendsChat":
                    updateLootChatStatus();
                    break;
            }
        }
    }

    @Subscribe
    /**
     * Parse chat messages to detect values and enqueue PendingValue suggestions.
     * @param event chat message event
     * @throws ParseException when number parsing fails
     */
    public void onChatMessage(ChatMessage event) throws ParseException {

        if (CheckChatJoinLeave(event)) {
            return;
        }//SIGH LOOK IM HERE

        Formats.OsrsAmountFormatter f = new Formats.OsrsAmountFormatter();

        if (!config.enableChatDetection()) {
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
        if (config.detectPvmValues()) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("^(.+?) has received a drop: .*?\\((\\d[\\d,]*) coins\\)").matcher(msg);
            if (m.find()) {
                String player = m.group(1);
                Long value = (Long) f.stringToValue(m.group(2));
                queuePending(PendingValue.Type.PVM, isClan ? "Clan" : "Friends", msg, value, player);
                return;
            }
        }

        // Try parse PvP loot
        if (config.detectPvpValues()) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("^(.+?) has defeated (.+?) and received \\((\\d[\\d,]*) coins\\) worth of loot!").matcher(msg);
            if (m.find()) {
                String player = m.group(1);
                Long value = (Long) f.stringToValue(m.group(3));
                queuePending(PendingValue.Type.PVP, isClan ? "Clan" : "Friends", msg, value, player);
                return;
            }
        }

        // Try parse player !add value
        //TODO specify add! unit
        //TODO fix negative numbers
        //TODO this duplicate of format function  @SIGH
        if (config.detectPlayerValues()) {
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("(?i)!add\\s+([0-9][0-9,]*(?:\\.[0-9]+)?)\\s*(k|tousand|thousand|m|mil|mill|million|b|bil|bill|billion)?\\b")
                    .matcher(msg);

            if (m.find()) {
                String who = sender != null ? sender : "";
                String numberTxt = m.group(1);
                String unitTxt = m.group(2);

                // Strip commas and parse
                java.math.BigDecimal base = new java.math.BigDecimal(numberTxt.replace(",", ""));

                java.math.BigDecimal multiplier = java.math.BigDecimal.ONE;
                if (unitTxt != null) {
                    switch (unitTxt.toLowerCase()) {
                        case "k":
                        case "tousand"://possible accepted typo
                        case "thousand":
                            multiplier = new java.math.BigDecimal(1_000L);
                            break;

                        case "m":
                        case "mil":
                        case "mill":
                        case "million":
                            multiplier = new java.math.BigDecimal(1_000_000L);
                            break;

                        case "b":
                        case "bil":
                        case "bill":
                        case "billion":
                            multiplier = new java.math.BigDecimal(1_000_000_000L);
                            break;
                    }
                }
                long value = base.multiply(multiplier)
                        .setScale(0, java.math.RoundingMode.DOWN)
                        .longValue();

                queuePending(PendingValue.Type.ADD, isClan ? "Clan" : "Friends", msg, value, who);
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
    private void queuePending(PendingValue.Type type, String source, String msg, Long value, String suggestedPlayer) {
        if (sessionManager == null) return;
        PendingValue pv = PendingValue.of(type, source, msg, value, suggestedPlayer);
        sessionManager.addPendingValue(pv);
        // Ask UI to refresh
        panel.refreshAllView();
    }

    private boolean CheckChatJoinLeave(ChatMessage event) {

        String plain = Text.removeTags(event.getMessage()).trim();
        String lower = plain.toLowerCase();

        ChatMessageType t = event.getType();
        boolean isSystemish = t == ChatMessageType.GAMEMESSAGE
                || t == ChatMessageType.CLAN_MESSAGE
                || t == ChatMessageType.CLAN_CHAT
                || t == ChatMessageType.CLAN_GUEST_CHAT
                || t.name().contains("CLAN");

        if (!isSystemish) {
            return false;
        }

        //LEAVE/KICK Chat
        if (isSystemish && java.util.regex.Pattern
                .compile("(?i)^\\s*(?:you\\s+(?:have\\s+)?left\\s+(?:the\\s+)?(?:chat-)?channel\\.?|you\\s+(?:are|aren't|are\\s+not)\\s+currently\\s+in\\s+(?:a|the|your)\\s+(?:chat-)?channel\\.?|you\\s+have\\s+been\\s+kicked\\s+from\\s+the\\s+channel\\.?)\\s*$")
                .matcher(plain)
                .find()) {
            chatExplicitKnown = true;
            chatExplicitOn = false;
            updateLootChatStatus();
            return true;
        }


        //JOIN Chat
        if (isSystemish && java.util.regex.Pattern
                .compile("(?i)^\\s*now\\s+talking\\s+in\\s+(?:the\\s+)?(?:chat-)?channel\\.?\\s*$")
                .matcher(plain)
                .find()) {
            chatExplicitKnown = true;
            chatExplicitOn = true;
            updateLootChatStatus();
            return false;
        }
        return false;
    }


    /**
     * Recompute overlay purely from member lists (no timers, no message heuristics).
     */
    private void updateLootChatStatus() {
        if (chatOverlay == null) return;

        boolean fcOn = isFriendsChatOn();
        boolean clanOn = isMainClanChatOn();
        boolean guestOn = isGuestClanChatOn();
        boolean counted =
                (config.detectInFriendsChat() && fcOn) ||
                        (config.detectInClanChat() && (clanOn || guestOn));

        chatOverlay.setVisible(true);
        chatOverlay.setStatuses(fcOn,
                clanOn,
                guestOn,
                counted);
    }


    @Subscribe
    /**
     * Track context (in game) menu openings to add an option to add/remove players from session.
     * This Triggers when you right click a player in the friends/clan chat.
     * @param event menu entry added event
     */
    public void onMenuEntryAdded(MenuEntryAdded event) {
        int componentId = event.getActionParam1();
        int groupId = WidgetUtil.componentToInterface(componentId);

        Session currentSession = null;
        if (sessionManager == null) return;
        else currentSession = sessionManager.getCurrentSession().orElse(null);
        if (currentSession == null) return;

        if (!(groupId == InterfaceID.FRIENDS || groupId == InterfaceID.CHATCHANNEL_CURRENT
                || componentId == InterfaceID.ClansSidepanel.PLAYERLIST || componentId == InterfaceID.ClansGuestSidepanel.PLAYERLIST))
            return;

        String playername = Text.removeTags(event.getTarget());

        if (sessionManager.currentSessionHasPlayer(playername)) {
            String removeFromSession = "Remove from session";

            // TODO Fix bug: For some reason this event/function triggers twice, so i have to check that the entry doesn't already exist' and i feel like i should not have to check this.
            // This might be a janky mess but idc
            if (Arrays.stream(client.getMenu().getMenuEntries()).anyMatch(e -> e.getOption().equals(removeFromSession)))
                return;

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
            return;

        client.getMenu().createMenuEntry(-1)
                .setOption(removeFromSession)
                .setTarget(event.getTarget())
                .setType(MenuAction.RUNELITE)
                .onClick(e ->
                {
                    if (playerManager.addKnownPlayer(playername))
                        sessionManager.addPlayerToActive(playername);
                    panelManager.refreshAllView();
                });
    }


    /**
     * Local player's cleaned display name ("" if not ready).
     */
    private String myCleanName() {
        Player me = client.getLocalPlayer();
        if (me == null) return "";
        return net.runelite.client.util.Text.toJagexName(
                net.runelite.client.util.Text.removeTags(me.getName()));
    }

    /**
     * True iff the given clan channel currently contains *you*.
     */
    private boolean channelHasSelf(ClanChannel ch) {
        if (ch == null || ch.getMembers() == null) return false;
        String me = myCleanName();
        if (me.isEmpty()) return false;

        for (ClanChannelMember m : ch.getMembers()) {
            if (m == null) continue;
            String n = m.getName();
            if (n == null) continue;
            n = net.runelite.client.util.Text.toJagexName(
                    net.runelite.client.util.Text.removeTags(n));
            if (me.equalsIgnoreCase(n)) return true;
        }
        return false;
    }

    /**
     * Joined to main / guest chat-channel (based solely on your presence).
     */
    private boolean isMainClanChatOn() {
        return channelHasSelf(client.getClanChannel());
    }

    private boolean isGuestClanChatOn() {
        return channelHasSelf(client.getGuestClanChannel());
    }

    /**
     * Joined to Friends Chat ("Chat Channel")?
     */
    private boolean isFriendsChatOn() {
        FriendsChatManager fcm = client.getFriendsChatManager();
        if (fcm == null) {
            return false;
        }

        FriendsChatMember[] members = fcm.getMembers();
        if (members == null || members.length == 0) {
            return false;
        }

        String me = myCleanName();
        if (!me.isEmpty()) {
            for (FriendsChatMember m : members) {
                if (m == null) continue;
                String n = net.runelite.client.util.Text.toJagexName(
                        net.runelite.client.util.Text.removeTags(m.getName()));
                if (me.equalsIgnoreCase(n)) {
                    return true;
                }
            }
        }
        return true;
    }
}
