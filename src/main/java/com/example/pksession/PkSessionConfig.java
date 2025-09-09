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
    default String peepsCsv() { return ""; }

    @ConfigItem(
            keyName = KEY_PEEPS_CSV,
            name = "Peeps",
            description = "Comma-separated known players",
            hidden = true
    )
    void peepsCsv(String value);

    // Visible, user-facing config

    @ConfigItem(
            keyName = "showToasts",
            name = "Show toasts",
            description = "Show confirmation/info popups in the panel"
    )
    default boolean showToasts() { return true; }

    @ConfigItem(
            keyName = "decimalPlaces",
            name = "Split decimal places",
            description = "Number of decimal places to show for totals and splits"
    )
    default int decimalPlaces() { return 2; }

    @ConfigItem(
            keyName = "allowNegativeKills",
            name = "Allow negative kill values",
            description = "Permit entering negative kill values (e.g., adjustments)"
    )
    default boolean allowNegativeKills() { return true; }
}
