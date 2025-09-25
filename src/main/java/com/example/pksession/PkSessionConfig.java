package com.example.pksession;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

/**
 * Stores persistent data and JSON blobs via ConfigManager.
 */
@ConfigGroup(PkSessionConfig.GROUP)
public interface PkSessionConfig extends Config
{
    String GROUP = "pksession";

    String KEY_SESSIONS_JSON = "sessionsJson";
    String KEY_CURRENT_SESSION_ID = "currentSessionId";
    String KEY_HISTORY_LOADED = "historyLoaded";
    String KEY_PEEPS_CSV = "peepsCsv";

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
            keyName = KEY_PEEPS_CSV,
            name = "Peeps",
            description = "Comma-separated known players",
            hidden = true
    )
    default String knownPlayersCsv() { return ""; }

    @ConfigItem(
            keyName = KEY_PEEPS_CSV,
            name = "Peeps",
            description = "Comma-separated known players",
            hidden = true
    )
    void knownPlayersCsv(String value);

    // Visible, user-facing config

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
    @ConfigItem(
            keyName = "enableChatDetection",
            name = "Enable chat detection",
            description = "Detect values from clan/friends chat and queue them in a waitlist"
    )
    default boolean enableChatDetection() { return false; }

    @ConfigItem(
            keyName = "detectInClanChat",
            name = "Detect in Clan Chat",
            description = "Listen for values in clan chat"
    )
    default boolean detectInClanChat() { return true; }

    @ConfigItem(
            keyName = "detectInFriendsChat",
            name = "Detect in Friends Chat",
            description = "Listen for values in friends chat"
    )
    default boolean detectInFriendsChat() { return true; }

    @ConfigItem(
            keyName = "detectPvmValues",
            name = "Detect PvM values",
            description = "Queue values detected from PvM drop messages"
    )
    default boolean detectPvmValues() { return true; }

    @ConfigItem(
            keyName = "detectPvpValues",
            name = "Detect PvP values",
            description = "Queue values detected from PvP loot messages"
    )
    default boolean detectPvpValues() { return true; }

    @ConfigItem(
            keyName = "detectPlayerValues",
            name = "Detect player !add",
            description = "Allow players to queue values by sending !add {value}"
    )
    default boolean detectPlayerValues() { return true; }
    @ConfigItem(
            keyName = "autoApplyWhenInSession",
            name = "Auto-apply when in session",
            description = "Skip waitlist if suggested player (or its main) is already in the active session"
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
