package com.splitmanager.models;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

/**
 * A single kill record attributed to a player for a given session.
 */
@Getter
@Setter
public class Kill implements Serializable
{
	private final String sessionId;
    private String player;
    private Long amount;
	private final Instant at;

	public Kill(String sessionId, String player, Long amount, Instant at)
	{
		this.sessionId = sessionId;
		this.player = player;
		this.amount = amount;
		this.at = at;
	}
}
