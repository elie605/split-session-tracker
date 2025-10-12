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
import net.runelite.api.events.MenuOpened;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.WidgetUtil;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.menus.MenuManager;
import net.runelite.client.menus.WidgetMenuOption;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.util.Text;

import java.awt.image.BufferedImage;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

@Slf4j
@PluginDescriptor(
        name = "Auto Split Manager",
        description = "Automatic split manager for group sessions. Tracks sessions, roster changes, chat-detected values, editable recent splits, and settlement metrics (copy JSON). Collapsible sections and configurable panel order.",
        enabledByDefault = true
)
public class ManagerPlugin extends Plugin {
    @Inject
    private Client client;
    @Inject
    private ClientToolbar clientToolbar;
    @Inject
    private PluginConfig config;

    // Return nullable panel; callers must handle null (e.g., during startup/shutdown)
    @Getter
    private static ManagerPanel panel;
    private NavigationButton navButton;
    private ManagerSession sessionManager;

    @Override
    protected void startUp() {
        sessionManager = new ManagerSession(config);
        sessionManager.loadFromConfig(); // load sessions and players (peeps)

        panel = new ManagerPanel(sessionManager, config);
        panel.refreshAllView();

        // TODO create an icon
        // Use a transparent placeholder icon so the panel shows in the side menu without bundling an image.
        BufferedImage placeholderIcon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);

        navButton = NavigationButton.builder()
                .tooltip("Auto Split Manager")
                .icon(placeholderIcon)
                .priority(5)
                .panel(panel)
                .build();
        clientToolbar.addNavigation(navButton);
    }


    @Override
    protected void shutDown() {
        if (navButton != null) {
            clientToolbar.removeNavigation(navButton);
            navButton = null;
        }
        if (sessionManager != null) {
            sessionManager.saveToConfig();
        }
        panel = null;
    }

    @Provides
    PluginConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(PluginConfig.class);
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged e) {
        if ("Split Manager".equals(e.getGroup()) && "directPayments".equals(e.getKey())) {
            log.info("Direct payments changed, refreshing panel");
            Utils.requestRestart().run();
        }
    }

    @Subscribe
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


    private void queuePending(PendingValue.Type type, String source, String msg, Long value, String suggestedPlayer) {
        if (sessionManager == null) return;
        PendingValue pv = PendingValue.of(type, source, msg, value, suggestedPlayer);
        sessionManager.addPendingValue(pv);
        // Ask UI to refresh
        Utils.requestUiRefresh().run();
    }

    @Subscribe
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


    //Yoinked
    private ChatPlayer getChatPlayerFromName(String name) {
        String cleanName = Text.removeTags(name);

        // Search friends chat members first, because we can always get their world;
        // friends worlds may be hidden if they have private off. (#5679)
        FriendsChatManager friendsChatManager = client.getFriendsChatManager();
        if (friendsChatManager != null) {
            FriendsChatMember member = friendsChatManager.findByName(cleanName);
            if (member != null) {
                return member;
            }
        }

        ClanChannel clanChannel = client.getClanChannel();
        if (clanChannel != null) {
            ClanChannelMember member = clanChannel.findMember(cleanName);
            if (member != null) {
                return member;
            }
        }

        clanChannel = client.getGuestClanChannel();
        if (clanChannel != null) {
            ClanChannelMember member = clanChannel.findMember(cleanName);
            if (member != null) {
                return member;
            }
        }

        NameableContainer<Friend> friendContainer = client.getFriendContainer();
        if (friendContainer != null) {
            return friendContainer.findByName(cleanName);
        }

        return null;
    }
}
