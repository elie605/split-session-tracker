package com.example.pksession;

import com.example.pksession.model.Kill;
import com.example.pksession.model.Session;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Manages sessions, persistence, and all the logic for roster changes,
 * child sessions, and live split calculations.
 */
public class SessionManager {
    private final PkSessionConfig config;
    private Gson gson = new GsonBuilder().create();

    private final Map<String, Session> sessions = new LinkedHashMap<>();
    private String currentSessionId;
    private boolean historyLoaded;
    private final Set<String> peeps = new LinkedHashSet<>();

    public SessionManager(PkSessionConfig config) {
        this.config = config;
        // Create a custom type adapter for Instant
        this.gson = new GsonBuilder()
                .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
                .create();
    }

    // Custom type adapter for Instant to avoid reflection issues
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


    public void loadFromConfig() {
        // peeps
        peeps.clear();
        String csv = config.peepsCsv();
        if (csv != null && !csv.isEmpty()) {
            for (String p : csv.split(",")) {
                String t = p.trim();
                if (!t.isEmpty()) peeps.add(t);
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

    public void saveToConfig() {
        config.peepsCsv(String.join(",", peeps));
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
    public String exportAllSessionsJson()
    {
        // TODO Implement: return gson.toJson(sessions.values());
        return "";
    }

    /**
     * Placeholder for exporting a single session by id as JSON.
     * Returns a JSON string for the specified session or empty string if not found.
     */
    public String exportSessionJson(String sessionId)
    {
        // TODO Implement: Session s = sessions.get(sessionId); return s != null ? gson.toJson(s) : "";
        return "";
    }


    // Peeps management

    public Set<String> getPeeps() {
        return Collections.unmodifiableSet(peeps);
    }

    public boolean addPeep(String name) {
        boolean added = peeps.add(name.trim());
        if (added) saveToConfig();
        return added;
    }

    public boolean removePeep(String name) {
        boolean rem = peeps.remove(name.trim());
        if (rem) saveToConfig();
        return rem;
    }

    // Sessions

    public Optional<Session> getCurrentSession() {
        return Optional.ofNullable(currentSessionId).map(sessions::get);
    }

    public List<Session> getAllSessionsNewestFirst() {
        return sessions.values().stream()
                .sorted(Comparator.comparing(Session::getStart).reversed())
                .collect(Collectors.toList());
    }

    public boolean isHistoryLoaded() {
        return historyLoaded;
    }

    public void unloadHistory() {
        historyLoaded = false;
        saveToConfig();
    }

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

    public boolean hasActiveSession() {
        return getCurrentSession().map(Session::isActive).orElse(false);
    }

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

    // Roster

    public boolean addPlayerToActive(String player) {
        if (historyLoaded) return false;
        Session curr = getCurrentSession().orElse(null);
        if (curr == null || !curr.isActive()) return false;

        player = player.trim();
        if (curr.getPlayers().contains(player)) return true;

        if (curr.hasKills()) {
            // Create a new child session, copy players, add this player, end current child
            String motherId = curr.getMotherId() == null ? curr.getId() : curr.getMotherId();
            Session newChild = new Session(newId(), Instant.now(), motherId);
            // copy players
            newChild.getPlayers().addAll(curr.getPlayers());
            // add the new player
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

    public boolean removePlayerFromActive(String player) {
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

    // Kills

    public boolean addKill(String player, double amount) {
        if (historyLoaded) return false;
        Session curr = getCurrentSession().orElse(null);
        if (curr == null || !curr.isActive()) return false;

        player = player.trim();
        if (!curr.getPlayers().contains(player)) return false;

        curr.getKills().add(new com.example.pksession.model.Kill(curr.getId(), player, amount, Instant.now()));
        saveToConfig();
        return true;
    }

    // Derived metrics

    public static class PlayerMetrics {
        public final String player;
        public final double total;
        public final double split;

        public PlayerMetrics(String player, double total, double split) {
            this.player = player;
            this.total = total;
            this.split = split;
        }
    }

    public List<PlayerMetrics> computeMetricsFor(Session s) {
        if (s == null) return List.of();
        Map<String, Double> totals = new LinkedHashMap<>();
        for (String p : s.getPlayers()) {
            totals.put(p, 0.0);
        }
        for (Kill k : s.getKills()) {
            totals.computeIfPresent(k.getPlayer(), (k1, v) -> v + k.getAmount());
        }
        double avg = totals.isEmpty() ? 0.0 : totals.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        List<PlayerMetrics> out = new ArrayList<>();
        for (Map.Entry<String, Double> e : totals.entrySet()) {
            double split = e.getValue() - avg; // mirrors your sheet's split formula
            out.add(new PlayerMetrics(e.getKey(), e.getValue(), split));
        }
        return out;
    }

    // Helpers

    private String newId() {
        return UUID.randomUUID().toString();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String emptyToNull(String s) {
        return s == null || s.isEmpty() ? null : s;
    }
}
