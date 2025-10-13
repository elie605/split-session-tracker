// TODO this is outdated 
# WORK IN PROGRESS
# Auto Split Manager

Auto Split Manager is an automatic split manager for RuneLite that helps small groups run fair and transparent group sessions.
It tracks your session roster, captures loot values from chat, and computes how to settle up at the end.

What does the Automatic loot splitter do?
- It aggregates per-player loot (K) across the session thread (mother + children sessions).
- For each session segment, it computes the roster average and each player’s delta from that average.
- Settlement shows, per player, how much they are up or down relative to the average, so you can pay or be paid to even out.
- Example: -100k means you owe that amount to the player; +100k means the player owes you that money.

Features
- Session management: start/stop; automatic child sessions on roster changes
- Roster management: add/remove known mains; alt→main linking
- Detected values (waitlist): parses PvM, PvP, and !add messages; always selects top entry
- One‑click apply from waitlist when the player is already in session (optional)
- Recent splits: dedicated, collapsible section with editable player and amount cells
- Settlement metrics: titled section with description; copy-to-clipboard (JSON) button
- Collapsible sections and configurable panel order (CSV in settings)

Settings highlights
- Toggle chat detection channels (Clan/Friends)
- Enable PvM/PvP parsing and player-submitted !add values
- Auto-apply when in session
- Panel section ordering (CSV)

TODO
- [ ] Export sessions to JSON (all or per-session)
- [ ] History editing (modify past sessions)
- [ ] Notifications/toasts for key actions

Getting started
1) Add known players (mains). Optionally link alts to mains.
2) Start a session and add players to the roster.
3) Add splits manually or let chat detection queue values in the Detected values section.
4) Review and edit Recent splits if needed; Settlement updates automatically.
5) Use Settlement to reconcile: negative means you owe; positive means they owe you.
