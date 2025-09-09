package com.example.pksession.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A session may have a motherId (root session) and child sessions created
 * when roster changes after kills happen.
 */
public class Session implements Serializable
{
	private String id;
	private Instant start;
	private Instant end; // null when active
	private String motherId; // null for mother; otherwise id of mother
	private final Set<String> players = new LinkedHashSet<>();
	private final List<Kill> kills = new ArrayList<>();

	public Session() {}

	public Session(String id, Instant start, String motherId)
	{
		this.id = id;
		this.start = start;
		this.motherId = motherId;
	}

	public String getId() { return id; }
	public Instant getStart() { return start; }
	public Instant getEnd() { return end; }
	public String getMotherId() { return motherId; }
	public Set<String> getPlayers() { return players; }
	public List<Kill> getKills() { return kills; }

	public void setEnd(Instant end) { this.end = end; }

	public boolean isActive() { return end == null; }

	public boolean hasKills()
	{
		return !kills.isEmpty();
	}
}
