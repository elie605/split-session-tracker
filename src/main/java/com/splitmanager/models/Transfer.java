package com.splitmanager.models;

import lombok.Getter;
import lombok.Setter;

// simple container for a suggested transfer
@Getter
@Setter
public final class Transfer {
    final String from;
    final String to;
    final long amount; // in coins (or whatever your split unit is)
    public Transfer(String from, String to, long amount) {
        this.from = from;
        this.to = to;
        this.amount = amount;
    }
}
