package com.splitmanager;

import com.splitmanager.models.Kill;
import com.splitmanager.models.Session;
import com.google.gson.*;
import com.splitmanager.models.PendingValue;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages sessions, persistence, and all the logic for roster changes,
 * child sessions, and live split calculations.
 */
public class ManagerSession {
    private final PluginConfig config;
    private final Gson gson;

    private final Map<String, Session> sessions = new LinkedHashMap<>();
    private String currentSessionId;
    @Getter
    private boolean historyLoaded;
    private final Set<String> knownPlayers = new LinkedHashSet<>();

    // Transient waitlist of detected values from chat
    private final java.util.List<PendingValue> pendingValues = new java.util.ArrayList<>();

    // Alt/main mapping (alt name -> main name)
    private final Map<String, String> altToMain = new LinkedHashMap<>();

    /**
     * Construct a new ManagerSession bound to the given PluginConfig.
     * This instance owns all in-memory session state and persists it via the config.
     * @param config backing configuration/store used to load and save state
     */
    public ManagerSession(PluginConfig config) {
        this.config = config;
        // Create a custom type adapter for Instant
        this.gson = new GsonBuilder()
                .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
                .create();
    }

    // Custom type adapter for Instant to avoid reflection issues
    /**
     * Gson adapter for java.time.Instant (ISO-8601 text), avoiding reflective access issues
     * on older JVMs/contexts. Stored as Instant.toString(), parsed with Instant.parse().
     */
    private static class InstantTypeAdapter implements JsonSerializer<Instant>, JsonDeserializer<Instant> {
        @Override
        public JsonElement serialize(Instant src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString());
        }

        @Override
        public Instant deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            return Instant.parse(json.getAsString());
        }
    }


    /**
     * Load sessions, known players, alt mappings, and current state from PluginConfig.
     */
    public void loadFromConfig() {
        // peeps
        knownPlayers.clear();
        String csv = config.knownPlayersCsv();
        if (csv != null && !csv.isEmpty()) {
            for (String p : csv.split(",")) {
                String t = p.trim();
                if (!t.isEmpty()) knownPlayers.add(t);
            }
        }
        // alts mapping
        altToMain.clear();
        String altsJson = config.altsJson();
        if (altsJson != null && !altsJson.isEmpty()) {
            try {
                java.lang.reflect.Type mapType = new com.google.gson.reflect.TypeToken<Map<String, String>>() {
                }.getType();
                Map<String, String> m = gson.fromJson(altsJson, mapType);
                if (m != null) altToMain.putAll(m);
            } catch (Exception ignored) {
            }
        }

        // sessions
        sessions.clear();
        String json = config.sessionsJson();
        if (json != null && !json.isEmpty()) {
            Session[] arr = gson.fromJson(json, Session[].class);
            if (arr != null) {
                for (Session s : arr) {
                    sessions.put(s.getId(), s);
                }
            }
        }

        currentSessionId = emptyToNull(config.currentSessionId());
        historyLoaded = config.historyLoaded();
    }

    /**
     * Persist sessions, current state, known players, and alt mappings to PluginConfig.
     */
    public void saveToConfig() {
        config.knownPlayersCsv(String.join(",", knownPlayers));
        // save alt mapping
        try {
            config.altsJson(gson.toJson(altToMain));
        } catch (Exception e) {
            // ignore
        }
        Session[] arr = sessions.values().toArray(new Session[0]);
        config.sessionsJson(gson.toJson(arr));
        config.currentSessionId(nullToEmpty(currentSessionId));
        config.historyLoaded(historyLoaded);
    }

    // TODO add export all/ or specific session to json

    /**
     * Placeholder for exporting all sessions as JSON (for sharing/backups).
     * Returns a JSON string. Implementation can reuse the existing gson and sessions map.
     */
    public String exportAllSessionsJson() {
        // TODO Implement: return gson.toJson(sessions.values());
        return "";
    }

    /**
     * Placeholder for exporting a single session by id as JSON.
     * Returns a JSON string for the specified session or empty string if not found.
     */
    public String exportSessionJson(String sessionId) {
        // TODO Implement: Session s = sessions.get(sessionId); return s != null ? gson.toJson(s) : "";
        return "";
    }

    /**
     * @return unmodifiable set of all known player names (mains and alts).
     */
    public Set<String> getKnownPlayers() {
        return Collections.unmodifiableSet(knownPlayers);
    }

    /**
     * Returns only main accounts (known players that are not linked as alts).
     */
    /**
     * @return unmodifiable set of known players that are mains (exclude names mapped as alts).
     */
    public Set<String> getKnownMains() {
        LinkedHashSet<String> mains = knownPlayers.stream()
                .filter(p -> !isAlt(p))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return Collections.unmodifiableSet(mains);
    }

    /**
     * Compute known main players not currently active in the session roster.
     * @return set of eligible names to add to the current session
     */
    public Set<String> getNonActivePlayers() {
        // Only mains should be offered for adding to session
        Session curr = getCurrentSession().orElse(null);
        java.util.Set<String> mains = getKnownMains();
        if (curr == null || !curr.isActive()) {
            return java.util.Collections.unmodifiableSet(mains);
        }
        return mains.stream()
                .filter(p -> !curr.getPlayers().contains(p))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }


    /**
     * Add a player name to the known-players list.
     * @param name display name
     * @return true if added
     */
    public boolean addKnownPlayer(String name) {
        boolean added = knownPlayers.add(name.trim());
        if (added) saveToConfig();
        return added;
    }

    public boolean removeKnownPlayer(String name) {
        String n = name == null ? null : name.trim();
        if (n == null || n.isEmpty()) return false;
        boolean rem = knownPlayers.remove(n);
        // Clean up alt/main mappings involving this name
        // Remove if this name is an alt
        altToMain.remove(n);
        // Remove any entries where this name is the main
        altToMain.entrySet().removeIf(e -> e.getValue().equalsIgnoreCase(n));
        // still persist mapping cleanup
        saveToConfig();
        return rem;
    }

    //TODO make sure this make sense to be option and fix it: Whyy is this optional??/
    /**
     * @return Optional of the currently active child session, if any. Empty if no session is active.
     */
    public Optional<Session> getCurrentSession() {
        return Optional.ofNullable(currentSessionId).map(sessions::get);
    }

    /**
     * @return all sessions (mother and children) sorted by start time descending (newest first).
     */
    public List<Session> getAllSessionsNewestFirst() {
        return sessions.values().stream()
                .sorted(Comparator.comparing(Session::getStart).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Exit read-only history mode and return to live mode.
     * Persists the flag immediately.
     */
    public void unloadHistory() {
        historyLoaded = false;
        saveToConfig();
    }

    /**
     * Enter read-only history mode by selecting a session to view.
     * Requires that no active session is running. Persists the flag immediately.
     * @param sessionId id of the session (mother or child) to load
     * @return the loaded session if found and preconditions met; empty otherwise
     */
    public Optional<Session> loadHistory(String sessionId) {
        if (hasActiveSession()) {
            return Optional.empty(); // must stop active first
        }
        Session s = sessions.get(sessionId);
        if (s == null) return Optional.empty();
        historyLoaded = true;
        saveToConfig();
        return Optional.of(s);
    }

    /**
     * @return true if there is a current child session and its end time is null (active).
     */
    public boolean hasActiveSession() {
        return getCurrentSession().map(Session::isActive).orElse(false);
    }

    /**
     * Start a new session thread consisting of a mother session and an initial active child.
     * Fails if history mode is on or another session is currently active.
     * @return the newly created active child session, if started
     */
    public Optional<Session> startSession() {
        if (historyLoaded) return Optional.empty();
        if (hasActiveSession()) return Optional.empty();

        // Create mother and an initial child immediately (to mirror sheet)
        Session mother = new Session(newId(), Instant.now(), null);
        sessions.put(mother.getId(), mother);

        Session child = new Session(newId(), Instant.now(), mother.getId());
        sessions.put(child.getId(), child);

        currentSessionId = child.getId();
        saveToConfig();
        return Optional.of(child);
    }

    /**
     * Stop the currently active child session. If its mother session is still active,
     * it will be ended as well. No-op in history mode.
     * @return true if an active session was stopped
     */
    public boolean stopSession() {
        if (historyLoaded) return false;

        Session curr = getCurrentSession().orElse(null);
        if (curr == null || !curr.isActive()) {
            return false;
        }
        curr.setEnd(Instant.now());

        // If child has a mother which is active, end mother too.
        if (curr.getMotherId() != null) {
            Session mother = sessions.get(curr.getMotherId());
            if (mother != null && mother.isActive()) {
                mother.setEnd(Instant.now());
            }
        }

        currentSessionId = null;
        saveToConfig();
        return true;
    }

    /**
     * Add a player to the currently active child session. If the child already has kills recorded,
     * a new child session is forked (same mother), roster is copied, the player is added, and the
     * previous child is ended to preserve historical rosters per split segment.
     * Alt names are resolved to main before checks. No-op in history mode.
     * @param player display name (main or alt)
     * @return true if the roster changed (player added)
     */
    public boolean addPlayerToActive(String player) {
        if (historyLoaded) return false; // TODO support his
        Session curr = getCurrentSession().orElse(null);
        if (curr == null || !curr.isActive()) return false;

        String mainPlayer = getMainName(player == null ? null : player.trim());
        if (mainPlayer == null || mainPlayer.isBlank()) return false;
        final String fMain = mainPlayer;
        if (curr.getPlayers().stream().anyMatch(p -> p.equalsIgnoreCase(fMain))) {
            // Player (main) already in session
            return false;
        }

        if (curr.hasKills()) {
            // Create a new child session, copy players, add this player, end current child
            String motherId = curr.getMotherId() == null ? curr.getId() : curr.getMotherId();
            Session newChild = new Session(newId(), Instant.now(), motherId);
            // copy players
            newChild.getPlayers().addAll(curr.getPlayers());
            // add the new player (main)
            newChild.getPlayers().add(player);

            // End current child (but keep kills)
            curr.setEnd(Instant.now());

            // Activate new child
            sessions.put(newChild.getId(), newChild);
            currentSessionId = newChild.getId();
        } else {
            curr.getPlayers().add(player);
        }
        saveToConfig();
        return true;
    }

    /**
     * Remove a player from the active child session. If the current child already has kills,
     * a new child is created (same mother) without this player, and the current child is ended
     * to keep per-segment rosters intact. No-op in history mode.
     * @param player display name (main or alt)
     * @return true if the roster changed (player removed)
     */
    public boolean removePlayerFromSession(String player) {
        if (historyLoaded) return false;
        Session curr = getCurrentSession().orElse(null);
        if (curr == null || !curr.isActive()) return false;

        player = player.trim();
        if (!curr.getPlayers().contains(player)) return false;

        if (curr.hasKills()) {
            // Create a new child without this player, end current child
            String motherId = curr.getMotherId() == null ? curr.getId() : curr.getMotherId();
            Session newChild = new Session(newId(), Instant.now(), motherId);
            String finalPlayer = player;
            newChild.getPlayers().addAll(
                    curr.getPlayers().stream().filter(p -> !p.equalsIgnoreCase(finalPlayer)).collect(Collectors.toList())
            );

        	curr.setEnd(Instant.now());
            sessions.put(newChild.getId(), newChild);
            currentSessionId = newChild.getId();
        } else {
            curr.getPlayers().remove(player);
        }
        saveToConfig();
        return true;
    }

    /**
     * Record a kill value for a player in the active session. The player is resolved to its main
     * and must be on the active roster. No-op in history mode.
     * @param player display name (main or alt)
     * @param amount value in coins (may be negative if allowed by config)
     * @return true if recorded
     */
    public boolean addKill(String player, Long amount) {
        if (historyLoaded) return false; //TODO support altering history
        Session curr = getCurrentSession().orElse(null);
        if (curr == null || !curr.isActive()) return false;

        String mainPlayer = getMainName(player == null ? null : player.trim());
        if (mainPlayer == null || mainPlayer.isBlank()) return false;
        if (curr.getPlayers().stream().noneMatch(p -> p.equalsIgnoreCase(mainPlayer))) return false;

        curr.getKills().add(new Kill(curr.getId(), mainPlayer, amount, Instant.now()));
        saveToConfig();
        return true;
    }

    // ===== Pending values (waitlist) =====
    /**
     * Read-only view of the queued pending values detected from chat.
     */
    public java.util.List<PendingValue> getPendingValues() {
        return java.util.Collections.unmodifiableList(pendingValues);
    }

    /**
     * Queue a new pending value. The suggested player is normalized to its main. If configured,
     * the value may be auto-applied (when the player is currently in session), in which case this
     * method records a kill and does not queue. A small cap prevents unbounded growth.
     * @param pv pending value payload; null is ignored
     */
    public void addPendingValue(PendingValue pv) {
        if (pv == null) return;
        // Normalize suggested player to main for all downstream uses
        String suggested = pv.getSuggestedPlayer();
        String resolved = getMainName(suggested);
        pv.setSuggestedPlayer(resolved);
        // Auto-add unknown suggested MAIN to known list
        if (resolved != null && !resolved.isBlank()) {
            if (!knownPlayers.contains(resolved)) {
                knownPlayers.add(resolved);
                saveToConfig();
            }
        }
        // Auto-apply if configured and player already in session
        if (config.autoApplyWhenInSession() && hasActiveSession() && resolved != null && !resolved.isBlank()) {
            Session curr = getCurrentSession().orElse(null);
            if (curr != null && curr.getPlayers().stream().anyMatch(p -> p.equalsIgnoreCase(resolved))) {
                addKill(resolved, pv.getValue());
                return; // do not queue
            }
        }
        // Limit size to avoid unbounded growth
        if (pendingValues.size() > 100) {
            pendingValues.remove(0);
        }
        pendingValues.add(pv);
    }

    /**
     * Remove a pending value by its id.
     * @param id unique pending id
     * @return true if removed
     */
    public boolean removePendingValueById(String id) {
        return pendingValues.removeIf(p -> p.getId().equals(id));
    }

    /**
     * Apply a pending value to a specific player and remove it from the queue.
     * The player is resolved to its main; the underlying addKill() enforces roster rules.
     * @param id pending id
     * @param player target player (main or alt)
     * @return true if applied
     */
    public boolean applyPendingValueToPlayer(String id, String player) {
        PendingValue pv = pendingValues.stream().filter(p -> p.getId().equals(id)).findFirst().orElse(null);
        if (pv == null) return false;
        String target = getMainName(player);
        boolean ok = addKill(target, pv.getValue());
        if (ok) {
            pendingValues.remove(pv);
        }
        return ok;
    }


    //TODO maybe create known player controller
    // ===== Alt/main mapping =====
    /**
     * Resolve a display name to its main account by following the alt->main mapping.
     * If the name is not an alt, returns the original trimmed name. Protects against cycles.
     * @param name main or alt name
     * @return resolved main name (or the input trimmed if not an alt)
     */
    public String getMainName(String name) {
        if (name == null) return null;
        String n = name.trim();
        String visited = null;
        // resolve chain up to a few steps to avoid cycles
        for (int i = 0; i < 5; i++) {
            String m = altToMain.get(n);
            if (m == null || m.equalsIgnoreCase(n)) return n;
            if (visited != null && visited.equalsIgnoreCase(m)) break;
            visited = n;
            n = m;
        }
        return n;
    }

    /**
     * @param name player name
     * @return true if the given name is present as a key in the alt->main mapping
     */
    public boolean isAlt(String name) {
        if (name == null) return false;
        return altToMain.containsKey(name.trim());
    }

    /**
     * List all alts currently linked to the given main.
     * @param main main account name
     * @return sorted list of alt names linked to this main (case-insensitive compare)
     */
    public java.util.List<String> getAltsOf(String main) {
        if (main == null || main.isBlank()) return java.util.List.of();
        String m = main.trim();
        java.util.List<String> out = new java.util.ArrayList<>();
        for (Map.Entry<String, String> e : altToMain.entrySet()) {
            if (e.getValue() != null && e.getValue().equalsIgnoreCase(m)) {
                out.add(e.getKey());
            }
        }
        out.sort(String::compareToIgnoreCase);
        return out;
    }

    /**
     * Validate whether an alt may be linked to a main.
     * Rules:
     * - alt and main must be non-empty and not equal (case-insensitive)
     * - the chosen main cannot itself be an alt
     * - the alt cannot already point to a different main
     * - the alt cannot be someone else's main (prevents cycles)
     * @return true if the link is allowed
     */
    public boolean canLinkAltToMain(String alt, String main) {
        if (alt == null || main == null) return false;
        String a = alt.trim();
        String m = main.trim();
        if (a.isEmpty() || m.isEmpty()) return false;
        if (a.equalsIgnoreCase(m)) return false; // cannot link self
        // main cannot be an alt
        if (altToMain.containsKey(m)) return false;
        // alt cannot already have a different main
        if (altToMain.containsKey(a) && !altToMain.get(a).equalsIgnoreCase(m)) return false;
        // alt cannot be a main of others (prevents main being alt through this change)
        for (Map.Entry<String, String> e : altToMain.entrySet()) {
            if (e.getValue() != null && e.getValue().equalsIgnoreCase(a)) return false;
        }
        return true;
    }

    /**
     * Attempt to persist an alt->main link, enforcing the validation from canLinkAltToMain().
     * Adds both names to the known list on success.
     * @return true if the mapping either already existed (same) or was created
     */
    public boolean trySetAltMain(String alt, String main) {
        if (!canLinkAltToMain(alt, main)) return false;
        String a = alt.trim();
        String m = main.trim();
        // no-op if already linked
        if (altToMain.containsKey(a) && altToMain.get(a).equalsIgnoreCase(m)) {
            return true;
        }
        altToMain.put(a, m);
        knownPlayers.add(a);
        knownPlayers.add(m);
        saveToConfig();
        return true;
    }

    /**
     * Unlink the given alt from its main.
     *
     * @param alt the alt name to unlink
     * @return true if an existing mapping was removed
     */
    public boolean unlinkAlt(String alt) {
        if (alt == null || alt.trim().isEmpty()) return false;
        String a = alt.trim();
        if (!altToMain.containsKey(a)) return false;
        altToMain.remove(a);
        saveToConfig();
        return true;
    }

    /**
     * Returns true if the given player (main or alt) is present in the roster of the current session.
     * Alts are resolved to their main before the check.
     */
    public boolean currentSessionHasPlayer(@Nonnull String player) {
        return sessionHasPlayer(player, getCurrentSession().orElse(null));
    }

    /**
     * Returns true if the given player (main or alt) is present in the roster of the provided session.
     * Alts are resolved to their main before the check.
     */
    public boolean sessionHasPlayer(@Nonnull String player, Session session) {
        if(isAlt(player)) {
            player = getMainName(player);
        }

        String finalPlayer = player;

        return session.getPlayers().stream().anyMatch(e ->
                e.equals(Objects.requireNonNull(finalPlayer)));
    }


    /**
     * Aggregate row to display per-player totals and split deltas for a session thread.
     * total = sum of that player's kills across all sessions in the thread.
     * split = sum over each session in the thread of (playerTotalInThatSession - avgOfThatSessionRoster).
     * activePlayer indicates whether the player is on the provided session's current roster.
     */
    public static class PlayerMetrics {
        public final String player;
        public final Long total;
        public final Long split;
        public final boolean activePlayer;

        public PlayerMetrics(String player, Long total, Long split, boolean activePlayer) {
            this.player = player;
            this.total = total;
            this.split = split;
            this.activePlayer = activePlayer;
        }
    }


    /**
     * Compute metrics for the given session's thread (mother + children) including only currently active players.
     * @param s a session within the thread to compute against
     * @return list of PlayerMetrics rows (non-zero totals only)
     */
    public List<PlayerMetrics> computeMetricsFor(Session s) {
        return computeMetricsFor(s, false);
    }

    //TODO make this not recalc everything if it does
    /**
     * Compute metrics for the given session's thread (mother + children).
     * When includeNonActivePlayers is true, any player appearing in the thread or known list may be included.
     * Otherwise, only players on the provided session's current roster are considered for output.
     * Players with zero total and zero split are omitted.
     * @param s a session within the thread to compute against
     * @param includeNonActivePlayers whether to include players outside the current roster
     * @return list of PlayerMetrics rows
     */
    public List<PlayerMetrics> computeMetricsFor(Session s, boolean includeNonActivePlayers) {
        if (s == null) return List.of();

        // Determine the mother (root) id for the session thread
        String rootId = (s.getMotherId() == null) ? s.getId() : s.getMotherId();

        // Collect all sessions in the thread: mother + all children
        List<Session> thread = new ArrayList<>();
        Session mother = sessions.get(rootId);
        if (mother != null) {
            thread.add(mother);
        }
        for (Session candidate : sessions.values()) {
            if (rootId.equals(candidate.getMotherId())) {
                thread.add(candidate);
            }
        }

        // Build included players:
        // - includeNonActivePlayers: union of knownPlayers and everyone who appeared in the thread
        // - otherwise: only the current session's active roster
        LinkedHashSet<String> includedPlayers = new LinkedHashSet<>();
        if (includeNonActivePlayers) {
            includedPlayers.addAll(knownPlayers);
            for (Session part : thread) {
                includedPlayers.addAll(part.getPlayers());
            }
        } else {
            includedPlayers.addAll(s.getPlayers());
        }

        // Initialize aggregate totals and splits
        Map<String, Long> totals = new LinkedHashMap<>();
        Map<String, Long> splits = new LinkedHashMap<>();
        for (String p : includedPlayers) {
            totals.put(p, 0L);
            splits.put(p, 0L);
        }

        // For each session in the thread:
        // - compute that session's per-player totals for its own roster
        // - compute that session's average across its roster (including zeroes)
        // - for players active in that session, accumulate (playerTotal - sessionAvg) into splits
        for (Session part : thread) {
            // Roster for this part (the only players eligible for this part's split)
            List<String> roster = new ArrayList<>(part.getPlayers());
            if (roster.isEmpty()) {
                continue;
            }

            // Per-session totals for players in this roster
            Map<String, Long> perSessionTotals = new LinkedHashMap<>();
            for (String p : roster) {
                perSessionTotals.put(p, 0L);
            }
            for (Kill k : part.getKills()) {
                perSessionTotals.computeIfPresent(k.getPlayer(), (k1, v) -> v + k.getAmount());
            }

            // Session average across the entire roster
            Long sessionAvg = 0L;
            if (!perSessionTotals.isEmpty()) {
                Long sum = 0L;
                for (Long v : perSessionTotals.values()) sum += v;
                sessionAvg = sum / perSessionTotals.size();
            }

            // Accumulate totals and splits into the aggregate maps
            for (Map.Entry<String, Long> e : perSessionTotals.entrySet()) {
                String player = e.getKey();
                Long playerTotalThisSession = e.getValue();

                // Aggregate total (only if we're showing this player)
                if (totals.containsKey(player)) {
                    totals.compute(player, (k, v) -> (v) + playerTotalThisSession);
                }

                // Aggregate split only for players active in THIS session
                if (splits.containsKey(player)) {
                    Long delta = playerTotalThisSession - sessionAvg;
                    splits.compute(player, (k, v) -> (v) + delta);
                }
            }
        }

        // Build output rows, marking active based on the provided session's current roster
        List<PlayerMetrics> out = new ArrayList<>();
        for (String p : includedPlayers) {
            boolean isActiveNow = s.getPlayers().contains(p);
            Long total = totals.getOrDefault(p, 0L);
            Long split = splits.getOrDefault(p, 0L);

            // Skip players with total = 0
            if (total == 0.0 && split == 0.0) {
                continue;
            }

            out.add(new PlayerMetrics(p, total, split, isActiveNow));
        }
        return out;
    }


    /**
     * Generate a random unique id for sessions.
     */
    private String newId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Utility: convert null to empty string for config storage.
     */
    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    /**
     * Utility: convert empty string to null when reading config values.
     */
    private static String emptyToNull(String s) {
        return s == null || s.isEmpty() ? null : s;
    }
}
