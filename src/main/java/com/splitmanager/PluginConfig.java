package com.splitmanager;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;


/**
 * The PluginConfig interface defines a configuration for the Split Manager plugin.
 * It provides persistent storage of various user and system-defined settings. Additionally, this interface
 * manages settings related to settlement, chat detection, and session handling.
 */
@ConfigGroup(PluginConfig.GROUP)
public interface PluginConfig extends Config
{
	String GROUP = "Split Manager";

	String KEY_SESSIONS_JSON = "sessionsJson";
	String KEY_CURRENT_SESSION_ID = "currentSessionId";
	String KEY_HISTORY_LOADED = "historyLoaded";
	String KEY_PEOPLE_CSV = "PlayersCsv";

	//TODO Create a new configitem that allows the user to submit any forms on enter, e.g. 1) user fills in split amount 2) presses enter 3) The same function as button press is called
	@ConfigSection(
		name = "Settlement",
		description = "Settlement config",
		position = 2
	)
	String settlementSection = "Settlement";
	// Chat detection settings
	@ConfigSection(
		name = "Chat detection",
		description = "Detect and queue values from chat",
		position = 1
	)
	String chatDetectionSection = "Chat detection";
	// Alt/main mapping persistence (hidden JSON)
	String KEY_ALTS_JSON = "altsJson";

	//TODO implement this
	//TODO implement this SIGHHHH I have claimed this
	//Added a weird thingy in Auto Split Manager -> Turn Chat Detection ON -> see top left corner for box
	//Automatically detect if Chat Channel is turned on, if not, warn player
	//Important because changing worlds may automatically turn Chat off
	//Option is a pop up, preferably in Canvas, alternatively closable pop up on the side
	@ConfigItem(
		keyName = "WarnNotInFC",
		name = "Warning not in FC",
		description = "Give a warning on OSRS canvas that you are not in a FC, usefull if you have !add on",
		hidden = true
	)
	default Boolean warnNotFC()
	{
		return false;
	}

	/**
	 * Retrieves the JSON string representation of serialized sessions.
	 *
	 * @return a JSON string representing the serialized sessions, or an empty string if no sessions are serialized
	 */
	@ConfigItem(
		keyName = KEY_SESSIONS_JSON,
		name = "Sessions JSON",
		description = "Serialized sessions",
		hidden = true
	)
	default String sessionsJson()
	{
		return "";
	}

	/**
	 * Sets the JSON string representation of serialized sessions.
	 *
	 * @param value a JSON string representing the serialized sessions
	 */
	@ConfigItem(
		keyName = KEY_SESSIONS_JSON,
		name = "Sessions JSON",
		description = "Serialized sessions",
		hidden = true
	)
	void sessionsJson(String value);

	/**
	 * Retrieves the identifier of the current active session.
	 *
	 * @return the current session ID as a string, or an empty string if no session is active
	 */
	@ConfigItem(
		keyName = KEY_CURRENT_SESSION_ID,
		name = "Current Session Id",
		description = "Active session id",
		hidden = true
	)
	default String currentSessionId()
	{
		return "";
	}

	/**
	 * Sets the identifier of the current active session.
	 *
	 * @param value the current session ID to set as a string
	 */
	@ConfigItem(
		keyName = KEY_CURRENT_SESSION_ID,
		name = "Current Session Id",
		description = "Active session id",
		hidden = true
	)
	void currentSessionId(String value);

	/**
	 * Determines if the UI is in a read-only state due to a selected historical session being loaded.
	 *
	 * @return true if the UI is read-only and the historical session is loaded, false otherwise
	 */
	@ConfigItem(
		keyName = KEY_HISTORY_LOADED,
		name = "History Loaded",
		description = "If true, UI is read-only; selected historical session is loaded",
		hidden = true
	)
	default boolean historyLoaded()
	{
		return false;
	}


	/**
	 * Sets whether the UI is in a read-only state due to a selected historical session being loaded.
	 *
	 * @param value true to set the UI to read-only and indicate that the historical session is loaded, false to disable it
	 */
	@ConfigItem(
		keyName = KEY_HISTORY_LOADED,
		name = "History Loaded",
		description = "If true, UI is read-only; selected historical session is loaded",
		hidden = true
	)
	void historyLoaded(boolean value);

	/**
	 * Retrieves a comma-separated string of known players.
	 *
	 * @return a string containing the known players separated by commas,
	 *         or an empty string if no players are defined
	 */
	@ConfigItem(
		keyName = KEY_PEOPLE_CSV,
		name = "Players",
		description = "Comma-separated known players",
		hidden = true
	)
	default String knownPlayersCsv()
	{
		return "";
	}

	/**
	 * Sets a comma-separated string of known players.
	 *
	 * @param value a string containing the known players separated by commas
	 */
	@ConfigItem(
		keyName = KEY_PEOPLE_CSV,
		name = "Players",
		description = "Comma-separated known players",
		hidden = true
	)
	void knownPlayersCsv(String value);

	// Settlement mode

	//todo fix
	@ConfigItem(
		keyName = "useActivePlayerManagement",
		name = "Use active player buttons",
		description = "Show top section with per-player buttons for adding splits/removing players"
	)
	default boolean useActivePlayerManagement()
	{
		return true;
	}

	/**
	 * Determines whether the Markdown table should be wrapped in triple backticks (` ``` `)
	 * and columns padded for monospaced display when copying the table for Discord.
	 *
	 * @return true if the table should be formatted for Discord with Markdown wrapping
	 * and monospaced column padding; false otherwise.
	 */
	// Markdown / copy settings
	@ConfigItem(
		keyName = "copyForDiscord",
		name = "Copy for Discord",
		description = "Wrap copied Markdown table in ``` and pad columns for monospaced display",
		section = settlementSection
	)
	default boolean copyForDiscord()
	{
		return true;
	}

	/**
	 * Determines whether direct payments between players are enabled, bypassing a middleman or bank.
	 * If enabled, the settlement guidance assumes players directly settle payments with one another.
	 * If disabled, a middleman or bank is used for settling payments.
	 *
	 * @return true if direct payments are enabled, false if middleman mode is used.
	 */
	@ConfigItem(
		keyName = "directPayments",
		name = "Direct payments (no middleman)",
		description = "If enabled, settlement guidance assumes players pay each other directly instead of settling via a bank/middleman. Off = middleman mode.",
		section = settlementSection
	)
	default boolean directPayments()
	{
		return false;
	}

	//TODO implement this
	@ConfigItem(
		keyName = "showToasts",
		name = "Show toasts",
		description = "Show confirmation/info popups in the panel"
	)
	default boolean showToasts()
	{
		return true;
	}

	//TODO implement this
	@ConfigItem(
		keyName = "allowNegativeKills",
		name = "Allow negative kill values",
		description = "Permit entering negative kill values (e.g., adjustments)"
	)
	default boolean allowNegativeKills()
	{
		return true;
	}

	/**
	 * Determines whether chat detection is enabled.
	 * This method checks if values from clan or friends chat should be detected
	 * and queued in a waitlist as specified by the configuration.
	 *
	 * @return true if chat detection is enabled; false otherwise
	 */
	@ConfigItem(
		keyName = "enableChatDetection",
		name = "Enable chat detection",
		description = "Detect values from clan/friends chat and queue them in a waitlist",
		section = chatDetectionSection
	)
	default boolean enableChatDetection()
	{
		return false;
	}

	/**
	 * Determines whether the detection of values should be enabled in the clan chat.
	 *
	 * @return true if detection in the clan chat is enabled, otherwise false.
	 */
	@ConfigItem(
		keyName = "detectInClanChat",
		name = "Detect in Clan Chat",
		description = "Listen for values in clan chat",
		section = chatDetectionSection
	)
	default boolean detectInClanChat()
	{
		return true;
	}

	/**
	 * Determines whether the system should listen for values in the friends chat.
	 *
	 * @return true if listening for values in friends chat is enabled; false otherwise.
	 */
	@ConfigItem(
		keyName = "detectInFriendsChat",
		name = "Detect in Friends Chat",
		description = "Listen for values in friends chat",
		section = chatDetectionSection
	)
	default boolean detectInFriendsChat()
	{
		return true;
	}

	/**
	 * Determines whether the detection of PvM (Player vs Monster) values
	 * from drop messages is enabled. When enabled, values related to
	 * PvM drops are queued for further processing.
	 *
	 * @return true if PvM value detection is enabled, false otherwise
	 */
	@ConfigItem(
		keyName = "detectPvmValues",
		name = "Detect PvM values",
		description = "Queue values detected from PvM drop messages",
		section = chatDetectionSection
	)
	default boolean detectPvmValues()
	{
		return true;
	}

	/**
	 * Determines if the detection of PvP values from loot messages should
	 * be enabled. When enabled, it queues the values detected from PvP
	 * loot messages for further processing.
	 *
	 * @return true if PvP value detection is enabled, false otherwise
	 */
	@ConfigItem(
		keyName = "detectPvpValues",
		name = "Detect PvP values",
		description = "Queue values detected from PvP loot messages",
		section = chatDetectionSection
	)
	default boolean detectPvpValues()
	{
		return true;
	}

	/**
	 * Enables or disables the detection of player values when they send a command
	 * using the format !add {value}.
	 *
	 * @return true if the detection of player values is enabled, false otherwise
	 */
	@ConfigItem(
		keyName = "detectPlayerValues",
		name = "Detect player !add",
		description = "Allow players to queue values by sending !add {value}",
		section = chatDetectionSection
	)
	default boolean detectPlayerValues()
	{
		return true;
	}

	/**
	 * Determines if the system should automatically apply when the suggested player
	 * (or their main account) is already in the active session, bypassing the waitlist.
	 *
	 * @return true if auto-apply is enabled when the player is in session; false otherwise
	 */
	@ConfigItem(
		keyName = "autoApplyWhenInSession",
		name = "Auto-apply when in session",
		description = "Skip waitlist if suggested player (or its main) is already in the active session",
		section = chatDetectionSection
	)
	default boolean autoApplyWhenInSession()
	{
		return false;
	}

	/**
	 * Retrieves the default section order as a comma-separated values string.
	 * The order specifies the arrangement of various sections such as session,
	 * session players, add split, recent splits, detected values, settlement,
	 * and known players.
	 *
	 * @return A string containing the comma-separated default order of the sections.
	 */
	@ConfigItem(
		keyName = "sectionOrderCsv",
		name = "Panel section order",
		description = "Comma-separated order of sections: session,sessionPlayers,addSplit,recentSplits,detectedValues,settlement,knownPlayers"
	)
	default String sectionOrderCsv()
	{
		return "session,sessionPlayers,addSplit,recentSplits,detectedValues,settlement,knownPlayers";
	}

	/**
	 * Sets the order of panel sections based on the provided comma-separated values.
	 *
	 * @param value a comma-separated string defining the order of sections.
	 *              Accepted values: session, sessionPlayers, addSplit, recentSplits, detectedValues, settlement, knownPlayers.
	 */
	@ConfigItem(
		keyName = "sectionOrderCsv",
		name = "Panel section order",
		description = "Comma-separated order of sections: session,sessionPlayers,addSplit,recentSplits,detectedValues,settlement,knownPlayers"
	)
	void sectionOrderCsv(String value);

	/**
	 * Indicates whether to flip the sign of settlement values for display purposes.
	 * When disabled, a positive value indicates that the bank pays the player.
	 * When enabled, a positive value indicates that the player pays the bank. This setting
	 * applies only in middleman mode and does not affect the actual transaction values.
	 *
	 * @return true if settlement sign flipping is enabled, false otherwise.
	 */
	@ConfigItem(
		keyName = "flipSettlementSign",
		name = "Flip settlement sign (perspective)",
		description = "Display-only: flips the sign of Split values. Off = + means bank pays the player; On = + means player pays the bank (middleman mode only)."
	)
	default boolean flipSettlementSign()
	{
		return false;
	}

	/**
	 * Retrieves a JSON-formatted string that represents the mapping of alternate accounts (alts) to their main account names.
	 * This configuration item is hidden and intended for internal use.
	 *
	 * @return a JSON string containing the alt-to-main name mapping. Defaults to an empty string if not configured.
	 */
	@ConfigItem(
		keyName = KEY_ALTS_JSON,
		name = "Alts JSON",
		description = "alt->main name mapping",
		hidden = true
	)
	default String altsJson()
	{
		return "";
	}

	/**
	 * Sets the JSON string representing an alt-to-main name mapping.
	 *
	 * @param value a JSON string defining the mapping from alternate accounts to main accounts
	 */
	@ConfigItem(
		keyName = KEY_ALTS_JSON,
		name = "Alts JSON",
		description = "alt->main name mapping",
		hidden = true
	)
	void altsJson(String value);
}
