package com.example.pksession.model;

import java.io.Serializable;
import java.time.Instant;

/**
 * A single kill record attributed to a player for a given session.
 */
public class Kill implements Serializable
{
	private String sessionId;
	private String player;
	private double amount;
	private Instant at;

	public Kill() {}

	public Kill(String sessionId, String player, double amount, Instant at)
	{
		this.sessionId = sessionId;
		this.player = player;
		this.amount = amount;
		this.at = at;
	}

	public String getSessionId() { return sessionId; }
	public String getPlayer() { return player; }
	public double getAmount() { return amount; }
	public Instant getAt() { return at; }
}
