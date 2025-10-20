
# Auto Split Manager

A RuneLite plugin for automatically managing and tracking splits in group activities.

## Features

- **Session Management**: Create, track, and archive split sessions with roster changes
- **Automatic Value Detection**: Detect values from chat channels (Clan/Friends Chat) and queue them
- **Player Management**: Maintain roster of active players with support for alt accounts
- **Split Calculation**: Calculate fair splits based on session participation
- **Settlement Guidance**: View detailed metrics to settle payments between players
- **Customizable UI**: Collapsible sections with configurable panel order

## How It Works

The plugin tracks split sessions, allowing you to add and remove players as your group changes. It automatically detects values from:

- PvM drop messages
- PvP loot messages
- Player `!add` commands in chat

These values can be manually approved or automatically applied to players who are already in your active session.

## Configuration Options

### General Settings
- **Active player management**: Show section with per-player buttons for adding splits/removing players
- **Show toasts**: Enable confirmation/info popups in the panel
- **Allow negative values**: Permit entering negative kill values for adjustments (This is an alternative to removing splits)
- **Panel section order**: Customize the order of UI sections in the panel

### Settlement Settings
- **Copy for Discord**: Format Markdown tables with proper spacing for Discord
- **Direct payments**: Toggle between direct player payments or using a middleman/bank
- **Flip settlement sign**: Change perspective of settlement values (+ means bank pays player vs. player pays bank)

### Chat Detection Settings
- **Enable chat detection**: Master toggle for detecting values from chat
- **Detect in Clan/Friends Chat**: Choose which chat channels to monitor
- **Detect PvM/PvP values**: Toggle automatic detection of drop and loot messages
- **Detect player !add**: Allow players to queue values via chat commands
- **Auto-apply when in session**: Automatically apply detected values for players already in session

## Alt Account Management

The plugin supports linking alt accounts to main accounts, ensuring that values are properly attributed regardless of which character is active. All alts are automatically resolved to their main accounts when calculating splits.

## Usage Tips

1. Start a new session when beginning group activities
2. Add all participating players to the session
3. Enable chat detection to automatically queue split values
4. Approve values from the waitlist or manually add splits
5. View settlement calculations when it's time to distribute funds
6. Save or export session data for record keeping

## Contribute

This plugin is under active development. Contributions, bug reports, and feature requests are welcome.