package com.example.pksession.model;

public final class SplitEntry {
    final String player;
    final long amountK; // normalized to K
    SplitEntry(String player, long amountK) { this.player = player; this.amountK = amountK; }
}