package com.example.pksession.model;

import lombok.Getter;
import lombok.Setter;

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
@Getter
public class Session implements Serializable
{
	private final String id;
	private final Instant start;
	@Setter
    private Instant end; // null when active
	private final String motherId; // null for mother; otherwise id of mother
	private final Set<String> players = new LinkedHashSet<>();
	private final List<Kill> kills = new ArrayList<>();

	public Session(String id, Instant start, String motherId)
	{
		this.id = id;
		this.start = start;
		this.motherId = motherId;
	}

    public boolean isActive() { return end == null; }

	public boolean hasKills()
	{
		return !kills.isEmpty();
	}
}
