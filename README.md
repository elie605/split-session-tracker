# Auto Split Manager

A RuneLite plugin for automatically managing and tracking splits in group activities.

| Start the session, to start adding kills | Keep track of who owes what | Keep track of detected chat messages   |
| ------------- |:-------------:| -----:|
| <img width="324" height="401" alt="image" src="https://github.com/user-attachments/assets/bd8c4aa4-54d8-4c55-86a6-71c12d398edb" />| <img width="322" height="564" alt="image" src="https://github.com/user-attachments/assets/268324ca-448b-484a-8757-3013ee9e0713" />|<img width="309" height="277" alt="image" src="https://github.com/user-attachments/assets/f172a6e9-0f64-4da1-a2f2-a21fc0088877" />
|

## Features

- Automatic Value Detection: Detect drop values from chat channels (Clan/Friends Chat) and queue them  
   - Detects PvM drops: Detects drop values from PvM drop messages  
   - Detects PvP loot: Detects loot values from PvP loot messages  
   - Detects player !add: Detects values from player `!add` commands in Clan/Friends chat  
   - TODO -> Detects through item drop name: Detects values from through item drop names  
- Player Management: Keep track of current participating players with support for alt accounts  
- Split Calculation: Automatically calculates fair splits based on session participation  
- Settlement Guidance: View metrics to settle payments between players  

<details>
  <summary><h2>How It Works</h2></summary>

### Step 1: Start a session
Using the plugin panel, start a new session

### Step 2: Add players to your known player list
Option A: Go to your Chat/Friends/Clan channel, right-click a player and select `Add to session`  
Option B: In the plugin panel's `Known player info` section, type a player's name in the text box

### Step 3: Add players to your current session
Option A: If you used Step 2 Option A, the player is already added to the session  
Option B: In the `Add players to session` section, select the player from the dropdown and click `Add to session`

### Step 4: Add splits to the session
Manual method: In the `Add split to session` section, select a player, enter an amount (e.g. 300k, 3.4m, 1.23b), and click 'Add'  
Semi-automatic method:
1. In settings under `Chat Detection Settings`, enable `Chat detection` and disable `Auto-apply when in session`
2. When drops are announced in Chat, they'll appear in the `Detected values` section
3. Select the item and click `Add` (or `Del` to remove)

Fully automatic method:
1. In settings under `Chat Detection Settings`, enable both `Chat detection` and `Auto-apply when in session`
2. Drops will be automatically added for players in the session

### Step 5: Manage players in the session
Option A: Right-click a player in Chat/Friends/Clan channel and select `Remove from session`  
Option B: In the plugin panel's `Settlement` section, click the `X` next to a player's name

### Step 6: View settlements
Settlement calculations appear automatically in the plugin panel under the `Settlement` section

### Step 7: Stop the session
Click the Stop button in the plugin panel when finished

</details>

<details>
  <summary><h2>Configuration Options</h2></summary>

### General Settings
Enable tour: Enables the tour/explanation of the plugin  

Enable popout: Add/removes a button that allows the plugin to be shown in a new window  

Default value: The plugin allows the user to select the default value they want to use when inputing splits, meaning that if "m, aka a million" is selected 1 is calculated as 1,000,000.

### Settlement Settings
Copy for Discord: Format Markdown tables with proper spacing for Discord  

~Direct payments: Toggle between direct player payments or using a middleman/bank~ Disabled, broken :(  

Flip settlement sign: Change perspective of settlement values (+ means bank pays player vs. player pays bank)

### Chat Detection Settings
Enable chat detection: Master toggle for detecting values from chat  

Detect in Clan/Friends Chat: Choose which chat channels to monitor  

Detect PvM/PvP values: Toggle automatic detection of drop and loot messages  

Detect player !add: Allow players to queue values via chat commands  

Auto-apply when in session: Automatically apply detected values for players already in session

</details>

<details>
  <summary><h2>Alt Account Management</h2></summary>

The plugin supports linking alt accounts to main accounts, ensuring that values are properly attributed regardless of
which character is active. All alts are automatically resolved to their main accounts when calculating splits.

</details>


  <h2>Contribute</h2>

This plugin is under active development. Contributions, bug reports, and feature requests are welcome.
