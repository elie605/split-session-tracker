package com.example.pksession;

import com.example.pksession.model.Metrics;
import com.example.pksession.model.Session;

import javax.swing.*;

import com.example.pksession.panel.PanelView;
import net.runelite.client.ui.PluginPanel;

import java.awt.event.ActionEvent;
import java.util.Optional;

import static com.example.pksession.Utils.toast;

public class PkSessionPanel extends PluginPanel {

    private final com.example.pksession.SessionManager manager;
    private final PkSessionConfig config;
    private PanelView trackerPanelUI;

    public PkSessionPanel(SessionManager manager, PkSessionConfig config) {
        this.manager = manager;
        this.config = config;

        this.trackerPanelUI = new PanelView(manager, config);

        add(trackerPanelUI);

        trackerPanelUI.getBtnAddToSession().addActionListener(this::onAddPlayerToSession);
        //trackerPanelUI.getBtnRemoveFromSession().addActionListener(this::onRemoveFromSession);
        trackerPanelUI.getBtnAddPeep().addActionListener(this::onAddKnownPlayer);
        trackerPanelUI.getBtnRemovePeep().addActionListener(this::onRemovePeep);
        trackerPanelUI.getBtnStart().addActionListener(this::onStartSession);
        trackerPanelUI.getBtnStop().addActionListener(this::onStopSession);
        trackerPanelUI.getBtnAddKill().addActionListener(this::onAddKill);
        //        loadHistoryBtn.addActionListener(this::onLoadHistory);
        //        unloadHistoryBtn.addActionListener(this::onUnloadHistory);

        refresh();
    }

    private void onStartSession(ActionEvent e) {
        if (manager.isHistoryLoaded()) {
            toast(this,"Unload history first.");
            return;
        }
        if (manager.hasActiveSession()) {
            toast(this,"Active session exists.");
            return;
        }
        manager.startSession().ifPresent(s -> toast(this,"Session started."));
        Utils.requestUiRefresh().run();
    }

    private void onStopSession(ActionEvent e) {
        if (manager.isHistoryLoaded()) {
            toast(this,"Cannot stop while history loaded.");
            return;
        }
        if (manager.stopSession()) {
            Utils.requestUiRefresh().run();
            //TODO disable table if session stopped
            toast(this,"Session stopped.");
        } else {
            toast(this,"Failed to stop session.");
        }
    }

    private void onAddPlayerToSession(ActionEvent e) {
        String player = (String)  trackerPanelUI.getNotInCurrentSessionPlayerDropdown().getSelectedItem();
        if (player == null) {
            toast(this,"Select a player in dropdown.");
            return;
        }
        if (manager.addPlayerToActive(player)) {
            Utils.requestUiRefresh().run();
        } else {
            toast(this,"Failed to add player, player might already be in session.");
        }
    }

/*    private void onRemoveFromSession(ActionEvent e) {
        String player = (String) trackerPanelUI.getPeepDropdown().getSelectedItem();
        if (player == null) {
            toast(this,"Select a player in dropdown.");
            return;
        }
        if (manager.removePlayerFromActive(player)) {
            toast(this,"Player removed.");
            Utils.requestUiRefresh().run();
        } else {
            toast(this,"Failed to remove player.");
        }
    }*/

    private void onAddKill(ActionEvent e) {
        String player = (String) trackerPanelUI.getCurrentSessionPlayerDropdown().getSelectedItem();
        if (player == null) {
            toast(this,"Select a player.");
            return;
        }
        Object val = trackerPanelUI.getKillAmountField().getValue();
        double amt;
        try {
            amt = val == null ? Double.parseDouble(trackerPanelUI.getKillAmountField().getText()) : ((Number) val).doubleValue();
        } catch (Exception ex) {
            toast(this,"Invalid amount.");
            return;
        }
        if (manager.addKill(player, amt)) {
            trackerPanelUI.getKillAmountField().setText("");
            Utils.requestUiRefresh().run();
        } else {
            toast(this,"Failed to add kill (is player in session?).");
        }
    }

    private void onLoadHistory(ActionEvent e) {
        Session s = trackerPanelUI.getHistoryList().getSelectedValue();
        if (s == null) {
            toast(this,"Select a session to load.");
            return;
        }
        if (manager.hasActiveSession()) {
            toast(this,"Stop active session first.");
            return;
        }
        Optional<Session> loaded = manager.loadHistory(s.getId());
        toast(this,loaded.isPresent() ? "History loaded." : "Failed to load history.");
        Utils.requestUiRefresh().run();
    }

    private void onUnloadHistory(ActionEvent e) {
        if (!manager.isHistoryLoaded()) {
            toast(this,"Nothing to unload.");
            return;
        }
        manager.unloadHistory();
        toast(this,"History unloaded.");
        Utils.requestUiRefresh().run();
    }

    private void onAddKnownPlayer(ActionEvent e) {
        String name = trackerPanelUI.getNewPeepField().getText().trim();
        if (name.isEmpty()) {
            toast(this,"Enter a name.");
            return;
        }
        if (!manager.addKnownPlayer(name)) {
            toast(this,"Player already in list exists.");
            return;
        }
        // Clear and make the newly added name selected in the single dropdown
        trackerPanelUI.getNewPeepField().setText("");
        manager.saveToConfig();
        Utils.requestUiRefresh().run();
        trackerPanelUI.getKnownPlayersDropdown().setSelectedItem(name);
        trackerPanelUI.getNewPeepField().requestFocusInWindow();
    }

    private void onRemovePeep(ActionEvent e) {
        String selected = (String) trackerPanelUI.getKnownPlayersDropdown().getSelectedItem();
        if (selected == null) {
            toast(this,"Select a peep to remove.");
            return;
        }
        if (!manager.removeKnownPlayer(selected)) {
            toast(this,"Not found.");
            return;
        }
        manager.saveToConfig();
        Utils.requestUiRefresh().run();
    }

/*
    private static String sessionLabel(Session s) {
        String mother = s.getMotherId() == null ? "mother" : "child of " + s.getMotherId().substring(0, 8);
        String end = s.getEnd() == null ? "active" : TS.format(s.getEnd());
        return String.format("%s  [%s → %s]  (%s)",
                s.getId().substring(0, 8),
                TS.format(s.getStart()),
                end,
                mother
        );
    }
*/

    void refresh() {
        // Update the general dropdown from the Peeps list
        String[] peeps = manager.getKnownPlayers().toArray(new String[0]);

        trackerPanelUI.getKnownPlayersDropdown().setModel(new DefaultComboBoxModel<>(peeps));

        // Update the session player dropdown from the current session's players
        Session currentSession = manager.getCurrentSession().orElse(null);
        if (currentSession != null && currentSession.isActive()) {
            String[] sessionPlayers = currentSession.getPlayers().toArray(new String[0]);
            String[] notPeeps = manager.getNonActivePlayers().toArray(new String[0]);

            trackerPanelUI.getCurrentSessionPlayerDropdown().setModel(new DefaultComboBoxModel<>(sessionPlayers));
            trackerPanelUI.getCurrentSessionPlayerDropdown().setEnabled(true);
            trackerPanelUI.getNotInCurrentSessionPlayerDropdown().setModel(new DefaultComboBoxModel<>(notPeeps));
        } else {
            // Clear and disable the session player dropdown if no active session
            trackerPanelUI.getCurrentSessionPlayerDropdown().setModel(new DefaultComboBoxModel<>(new String[0]));
            trackerPanelUI.getCurrentSessionPlayerDropdown().setEnabled(false);
        }

        // History label
        trackerPanelUI.getHistoryLabel().setText("History: " + (manager.isHistoryLoaded() ? "ON" : "OFF"));

        // Table data
        // TODO check and fix?
        Session current = manager.getCurrentSession().orElse(null);
        if (current != null) {
            ((Metrics) trackerPanelUI.getMetricsTable().getModel()).setData(current, manager.computeMetricsFor(current, true));
        }

        // Update recent splits table with the last 10 kills from the current session
        if (current != null) {
            trackerPanelUI.getRecentSplitsModel().setFromKills(current.getKills());
        } else {
            trackerPanelUI.getRecentSplitsModel().clear();
        }

        // Enable/disable based on history
        boolean ro = manager.isHistoryLoaded();
        trackerPanelUI.getBtnStart().setEnabled(!ro && !manager.hasActiveSession());
        trackerPanelUI.getBtnStop().setEnabled(!ro && manager.hasActiveSession());
        trackerPanelUI.getBtnAddToSession().setEnabled(!ro && manager.hasActiveSession());
        trackerPanelUI.getNotInCurrentSessionPlayerDropdown().setEnabled(!ro && manager.hasActiveSession());
        trackerPanelUI.getBtnRemoveFromSession().setEnabled(!ro && manager.hasActiveSession());
        trackerPanelUI.getBtnAddKill().setEnabled(!ro && manager.hasActiveSession() && trackerPanelUI.getCurrentSessionPlayerDropdown().getItemCount() > 0);
        trackerPanelUI.getKillAmountField().setEnabled(!ro && manager.hasActiveSession() && trackerPanelUI.getCurrentSessionPlayerDropdown().getItemCount() > 0);
    }

}