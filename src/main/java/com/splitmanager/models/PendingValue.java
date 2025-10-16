package com.splitmanager.models;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PendingValue
{
	private String id;
	private Type type;
	private String source; // e.g., Clan, Friends
	private String message; // original or summary
	private Long value;
	private String suggestedPlayer; // may be null
	private Instant detectedAt;

	public static PendingValue of(Type type, String source, String message, Long value, String suggestedPlayer)
	{
		return new PendingValue(UUID.randomUUID().toString(), type, source, message, value, suggestedPlayer, Instant.now());
	}

	public enum Type
	{PVM, PVP, ADD}
}
