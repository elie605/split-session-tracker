package com.splitmanager.models;

import java.io.Serializable;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/**
 * A single kill record attributed to a player for a given session.
 */
@Getter
@Setter
public class Kill implements Serializable
{
	private final String sessionId;
	private final Instant at;
	private String player;
	private Long amount;

	public Kill(String sessionId, String player, Long amount, Instant at)
	{
		this.sessionId = sessionId;
		this.player = player;
		this.amount = amount;
		this.at = at;
	}
}
