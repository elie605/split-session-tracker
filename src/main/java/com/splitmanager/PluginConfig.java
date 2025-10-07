package com.splitmanager;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

/**
 * Stores persistent data and JSON blobs via ConfigManager.
 */
@ConfigGroup(PluginConfig.GROUP)
public interface PluginConfig extends Config
{
    String GROUP = "Split Manager";

    String KEY_SESSIONS_JSON = "sessionsJson";
    String KEY_CURRENT_SESSION_ID = "currentSessionId";
    String KEY_HISTORY_LOADED = "historyLoaded";
    String KEY_PEOPLE_CSV = "peepsCsv";

    //TODO implement this
    @ConfigItem(
            keyName = "WarnNotInFC",
            name = "Warning not in FC",
            description = "Give a warning on OSRS canvas that you are not in a FC, usefull if you have !add on",
            hidden = true
    )
    default Boolean warnNotFC() { return false; }

    @ConfigItem(
            keyName = KEY_SESSIONS_JSON,
            name = "Sessions JSON",
            description = "Serialized sessions",
            hidden = true
    )
    default String sessionsJson() { return ""; }

    @ConfigItem(
            keyName = KEY_SESSIONS_JSON,
            name = "Sessions JSON",
            description = "Serialized sessions",
            hidden = true
    )
    void sessionsJson(String value);

    @ConfigItem(
            keyName = KEY_CURRENT_SESSION_ID,
            name = "Current Session Id",
            description = "Active session id",
            hidden = true
    )
    default String currentSessionId() { return ""; }

    @ConfigItem(
            keyName = KEY_CURRENT_SESSION_ID,
            name = "Current Session Id",
            description = "Active session id",
            hidden = true
    )
    void currentSessionId(String value);

    @ConfigItem(
            keyName = KEY_HISTORY_LOADED,
            name = "History Loaded",
            description = "If true, UI is read-only; selected historical session is loaded",
            hidden = true
    )
    default boolean historyLoaded() { return false; }

    @ConfigItem(
            keyName = KEY_HISTORY_LOADED,
            name = "History Loaded",
            description = "If true, UI is read-only; selected historical session is loaded",
            hidden = true
    )
    void historyLoaded(boolean value);

    @ConfigItem(
            keyName = KEY_PEOPLE_CSV,
            name = "Peeps",
            description = "Comma-separated known players",
            hidden = true
    )
    default String knownPlayersCsv() { return ""; }

    @ConfigItem(
            keyName = KEY_PEOPLE_CSV,
            name = "Peeps",
            description = "Comma-separated known players",
            hidden = true
    )
    void knownPlayersCsv(String value);

    // Visible, user-facing config

    //todo fix
    @ConfigItem(
            keyName = "useActivePlayerManagement",
            name = "Use active player buttons",
            description = "Show top section with per-player buttons for adding splits/removing players"
    )
    default boolean useActivePlayerManagement() { return true; }

    @ConfigSection(
            name = "Settlement",
            description = "Settlement config",
            position = 2
    )
    String settlementSection = "Settlement";

    // Markdown / copy settings
    @ConfigItem(
            keyName = "copyForDiscord",
            name = "Copy for Discord",
            description = "Wrap copied Markdown table in ``` and pad columns for monospaced display",
            section = settlementSection
    )
    default boolean copyForDiscord() { return true; }

    // Settlement mode

    @ConfigItem(
            keyName = "directPayments",
            name = "Direct payments (no middleman)",
            description = "If enabled, settlement guidance assumes players pay each other directly instead of settling via a bank/middleman. Off = middleman mode.",
            section = settlementSection
    )
    default boolean directPayments() {
        return false;
    }

    //TODO implement this
    @ConfigItem(
            keyName = "showToasts",
            name = "Show toasts",
            description = "Show confirmation/info popups in the panel"
    )
    default boolean showToasts() { return true; }

    //TODO implement this
    @ConfigItem(
            keyName = "allowNegativeKills",
            name = "Allow negative kill values",
            description = "Permit entering negative kill values (e.g., adjustments)"
    )
    default boolean allowNegativeKills() { return true; }
    
    // Chat detection settings
    @ConfigSection(
            name = "Chat detection",
            description = "Detect and queue values from chat",
            position = 1
    )
    String chatDetectionSection = "Chat detection";

    @ConfigItem(
            keyName = "enableChatDetection",
            name = "Enable chat detection",
            description = "Detect values from clan/friends chat and queue them in a waitlist",
            section = chatDetectionSection
    )
    default boolean enableChatDetection() { return false; }

    @ConfigItem(
            keyName = "detectInClanChat",
            name = "Detect in Clan Chat",
            description = "Listen for values in clan chat",
            section = chatDetectionSection
    )
    default boolean detectInClanChat() { return true; }

    @ConfigItem(
            keyName = "detectInFriendsChat",
            name = "Detect in Friends Chat",
            description = "Listen for values in friends chat",
            section = chatDetectionSection
    )
    default boolean detectInFriendsChat() { return true; }

    @ConfigItem(
            keyName = "detectPvmValues",
            name = "Detect PvM values",
            description = "Queue values detected from PvM drop messages",
            section = chatDetectionSection
    )
    default boolean detectPvmValues() { return true; }

    @ConfigItem(
            keyName = "detectPvpValues",
            name = "Detect PvP values",
            description = "Queue values detected from PvP loot messages",
            section = chatDetectionSection
    )
    default boolean detectPvpValues() { return true; }

    @ConfigItem(
            keyName = "detectPlayerValues",
            name = "Detect player !add",
            description = "Allow players to queue values by sending !add {value}",
            section = chatDetectionSection
    )
    default boolean detectPlayerValues() { return true; }

    @ConfigItem(
            keyName = "autoApplyWhenInSession",
            name = "Auto-apply when in session",
            description = "Skip waitlist if suggested player (or its main) is already in the active session",
            section = chatDetectionSection
    )
    default boolean autoApplyWhenInSession() { return false; }

    // Panel order (optional, CSV of section keys)
    @ConfigItem(
            keyName = "sectionOrderCsv",
            name = "Panel section order",
            description = "Comma-separated order of sections: session,sessionPlayers,addSplit,recentSplits,detectedValues,settlement,knownPlayers"
    )
    default String sectionOrderCsv() { return "session,sessionPlayers,addSplit,recentSplits,detectedValues,settlement,knownPlayers"; }

    @ConfigItem(
            keyName = "sectionOrderCsv",
            name = "Panel section order",
            description = "Comma-separated order of sections: session,sessionPlayers,addSplit,recentSplits,detectedValues,settlement,knownPlayers"
    )
    void sectionOrderCsv(String value);



    @ConfigItem(
            keyName = "flipSettlementSign",
            name = "Flip settlement sign (perspective)",
            description = "Display-only: flips the sign of Split values. Off = + means bank pays the player; On = + means player pays the bank (middleman mode only)."
    )
    default boolean flipSettlementSign() { return false; }

    // Alt/main mapping persistence (hidden JSON)
    String KEY_ALTS_JSON = "altsJson";

    @ConfigItem(
            keyName = KEY_ALTS_JSON,
            name = "Alts JSON",
            description = "alt->main name mapping",
            hidden = true
    )
    default String altsJson() { return ""; }

    @ConfigItem(
            keyName = KEY_ALTS_JSON,
            name = "Alts JSON",
            description = "alt->main name mapping",
            hidden = true
    )
    void altsJson(String value);
}
