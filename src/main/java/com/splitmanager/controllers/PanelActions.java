package com.splitmanager.controllers;

public interface PanelActions {
    void startSession();

    void stopSession();

    void addPlayerToSession(String player);

    void addKnownPlayer(String name);

    void removeKnownPlayer(String name);

    void addKill(String player, long amount);

    void addAltToMain(String main, String alt);

    void removeSelectedAlt(String main, String selectedEntry);

    void applySelectedPendingValue(int tableRowIndex);

    void deleteSelectedPendingValue(int tableRowIndex);

    void onKnownPlayerSelectionChanged(String selected);

    void refreshAllView(); // idempotent, safe to call after model mutations
}
