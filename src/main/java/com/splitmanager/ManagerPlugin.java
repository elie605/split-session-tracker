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
import net.runelite.api.events.GameStateChanged;
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
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import net.runelite.client.util.Text;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.text.ParseException;
import java.util.Arrays;

//SIGH
import net.runelite.api.events.GameStateChanged;//login and hop listener, checks for world change
import net.runelite.api.events.ClanChannelChanged;
import net.runelite.api.events.FriendsChatChanged;
import net.runelite.api.events.WorldChanged;

import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.TitleComponent;
import net.runelite.client.ui.overlay.components.LineComponent;

import java.awt.Color;
import java.awt.Dimension;

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

    // Return nullable panel; callers must handle null (e.g., during startup/shutdown)
    @Getter
    private static ManagerPanel panel;
    private NavigationButton navButton;
    private ManagerSession sessionManager;

    @Override
    /**
     * Initialize plugin state and register the sidebar panel/navigation.
     */
    protected void startUp() {
        sessionManager = new ManagerSession(config);
        sessionManager.loadFromConfig(); // load sessions and players (peeps)

        panel = new ManagerPanel(sessionManager, config);
        panel.refreshAllView();

        // TODO create an icon
        // Use a transparent placeholder icon so the panel shows in the side menu without bundling an image.
        BufferedImage placeholderIcon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);

        //SIGH
        // --- Chat ON/OFF overlay ---
        chatOverlay = new ChatStatusOverlay(config);
        overlayManager.add(chatOverlay);
        updateLootChatStatus(); // initial compute

        navButton = NavigationButton.builder()
                .tooltip("Auto Split Manager")
                .icon(placeholderIcon)
                .priority(5)
                .panel(panel)
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
        if (chatOverlay != null)
        {
            overlayManager.remove(chatOverlay);
            chatOverlay = null;
        }

        panel = null;
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
    public void onClanChannelChanged(ClanChannelChanged e) {updateLootChatStatus();}
    @Subscribe
    public void onFriendsChatChanged(FriendsChatChanged e) {updateLootChatStatus();}
    @Subscribe
    public void onWorldChanged(WorldChanged e) {updateLootChatStatus();}


    @Subscribe
    /**
     * React to plugin configuration changes that require a panel refresh/restart.
     * @param e config change event
     */
    public void onConfigChanged(ConfigChanged e) {
        if ("Split Manager".equals(e.getGroup()) && "directPayments".equals(e.getKey())) {
            log.info("Direct payments changed, refreshing panel");
            Utils.requestRestart().run();
        }

        //SIGH
        if ("Split Manager".equals(e.getGroup()))
        {
            switch (e.getKey())
            {
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
        if (config.detectPlayerValues()) {
            // Accept forms like: !add 250k, !add 1.2m, !add 3b, or plain numbers (treated as K by formatter)
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("(?i)!add\\s+([0-9][0-9,]*(?:\\.[0-9]+)?\\s*[kmb]?)")
                    .matcher(msg);
            if (m.find()) {
                String who = sender != null ? sender : "";
                String amtText = m.group(1);
                Long valueK = (Long) f.stringToValue(amtText);
                queuePending(PendingValue.Type.ADD, isClan ? "Clan" : "Friends", msg, valueK, who);
            }
        }
    }


    /**
     * Enqueue a pending value suggestion for user approval.
     * @param type source type (PvM, PvP, player add)
     * @param source chat source label
     * @param msg original chat message
     * @param value numeric value (coins or K)
     * @param suggestedPlayer prefilled player name when available
     */
    private void queuePending(PendingValue.Type type, String source, String msg, Long value, String suggestedPlayer) {
        if (sessionManager == null) return;
        PendingValue pv = PendingValue.of(type, source, msg, value, suggestedPlayer);
        sessionManager.addPendingValue(pv);
        // Ask UI to refresh
        Utils.requestUiRefresh().run();
    }

    //SIGH START
    /** Joined to your main Clan Chat? */
    private boolean isMainClanChatOn()
    {
        ClanChannel main = client.getClanChannel();
        return main != null && main.getName() != null && !main.getName().isEmpty();
    }

    /** Joined to a Guest Clan Chat? */
    private boolean isGuestClanChatOn()
    {
        ClanChannel guest = client.getGuestClanChannel();
        return guest != null && guest.getName() != null && !guest.getName().isEmpty();
    }

    /** Joined to Friends Chat (legacy FC)? — robust across client versions */
    private boolean isFriendsChatOn()
    {
        FriendsChatManager fc = client.getFriendsChatManager();
        if (fc == null) return false;

        boolean hasName    = fc.getName() != null && !fc.getName().isEmpty();
        boolean hasMembers = fc.getMembers() != null && fc.getMembers().length > 0; // safest signal

        return hasName || hasMembers;
    }


    /** True if at least one configured loot-broadcast channel is joined. */
    private boolean anyLootBroadcastChannelOn()
    {
        boolean ccWanted = config.detectInClanChat();
        boolean fcWanted = config.detectInFriendsChat();

        boolean clanAny = isMainClanChatOn() || isGuestClanChatOn();
        boolean fcOn    = isFriendsChatOn();

        return (ccWanted && clanAny) || (fcWanted && fcOn);
    }

    /** Recompute and update overlay purely from in-game chat state (no config). */
    private void updateLootChatStatus()
    {
        if (chatOverlay == null)
        {
            return;
        }

        boolean fcOn    = isFriendsChatOn();
        boolean clanOn  = isMainClanChatOn();
        boolean guestOn = isGuestClanChatOn();

        // Overall status = any chat joined
        boolean anyOn = fcOn || clanOn || guestOn;

        chatOverlay.setVisible(true); // always show the overlay
        chatOverlay.setStatuses(fcOn, clanOn, guestOn, anyOn);

        // (Optional) Debug: see raw values the client reports
        // FriendsChatManager fc = client.getFriendsChatManager();
        // ClanChannel main = client.getClanChannel();
        // ClanChannel guest = client.getGuestClanChannel();
        // int fcMembers = (fc != null && fc.getMembers() != null) ? fc.getMembers().size() : 0;
        // log.debug("FC(name='{}', members={}) | Clan='{}' | Guest='{}' | anyOn={}",
        //         fc != null ? fc.getName() : null, fcMembers,
        //         main != null ? main.getName() : null,
        //         guest != null ? guest.getName() : null, anyOn);
    }


    //SIGH END

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
                        Utils.requestUiRefresh().run();
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
                    if (sessionManager.addKnownPlayer(playername))
                        sessionManager.addPlayerToActive(playername);
                    Utils.requestUiRefresh().run();
                });
    }


    //SIGH
    // Small multi-line overlay: shows FC / Clan / Guest Clan individually + overall status color
    final class ChatStatusOverlay extends OverlayPanel
    {
        private final PluginConfig config;
        private boolean visible = false;

        private boolean fcOn = false;
        private boolean clanOn = false;
        private boolean guestOn = false;
        private boolean countedOn = false; // respects toggles

        ChatStatusOverlay(PluginConfig config)
        {
            this.config = config;
            setPosition(OverlayPosition.TOP_LEFT);
            setLayer(OverlayLayer.ABOVE_SCENE);
        }

        void setVisible(boolean v) { this.visible = v; }

        /** fc/clan/guest raw states + combined "counted" state (based on config toggles) */
        void setStatuses(boolean fc, boolean clan, boolean guest, boolean counted)
        {
            this.fcOn = fc;
            this.clanOn = clan;
            this.guestOn = guest;
            this.countedOn = counted;
        }

        @Override
        public Dimension render(java.awt.Graphics2D g)
        {
            if (!visible)
            {
                return null;
            }

            panelComponent.getChildren().clear();
            panelComponent.setPreferredSize(new Dimension(230, 0));

            // Title color reflects overall counted state
            final String title = "Split Manager";
            final Color titleColor  = countedOn ? new Color(80, 220, 120)  : new Color(255, 80, 80);

            panelComponent.getChildren().add(TitleComponent.builder()
                    .text(title)
                    .color(titleColor)
                    .build());

            // Show each channel's raw state (always), with per-line color
            addStatusLine("Friends Chat", fcOn);
            addStatusLine("Clan Chat",    clanOn);
            addStatusLine("Guest Clan",   guestOn);

            // Summarize which chats are currently joined (actual game state)
            String joined = (fcOn ? "FC " : "") + (clanOn ? "Clan " : "") + (guestOn ? "Guest " : "");
            if (joined.isEmpty()) joined = "None";
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Joined")
                    .right(joined.trim())
                    .build());


            return super.render(g);
        }

        private void addStatusLine(String label, boolean on)
        {
            final String statusText = on ? "ON" : "OFF";
            final Color  statusCol  = on ? new Color(120, 255, 120) : new Color(255, 120, 120);

            panelComponent.getChildren().add(LineComponent.builder()
                    .left(label)
                    .right(statusText)
                    .leftColor(Color.WHITE)
                    .rightColor(statusCol)
                    .build());
        }


    }


    //SIGH
    //after new login or world hop update if chat is on or off
    @Subscribe
    public void onGameStateChanged(GameStateChanged e)
    {
        switch (e.getGameState())
        {
            case LOGGED_IN:
            case LOADING:
            case HOPPING:
                updateLootChatStatus();
                break;
            default:
                break;
        }
    }
}
