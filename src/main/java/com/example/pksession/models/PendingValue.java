package com.example.pksession.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PendingValue {
    public enum Type { PVM, PVP, ADD}

    private String id;
    private Type type;
    private String source; // e.g., Clan, Friends
    private String message; // original or summary
    private Long value;
    private String suggestedPlayer; // may be null
    private Instant detectedAt;

    public static PendingValue of(Type type, String source, String message, Long value, String suggestedPlayer) {
        return new PendingValue(UUID.randomUUID().toString(), type, source, message, value, suggestedPlayer, Instant.now());
        }
}
