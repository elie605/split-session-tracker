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
    private final PanelView trackerPanelUI;

    public PkSessionPanel(SessionManager manager, PkSessionConfig config) {
        this.manager = manager;

        this.trackerPanelUI = new PanelView(manager, config);

        add(trackerPanelUI);

        trackerPanelUI.getBtnAddToSession().addActionListener(this::onAddPlayerToSession);
        //trackerPanelUI.getBtnRemoveFromSession().addActionListener(this::onRemoveFromSession);
        trackerPanelUI.getBtnAddPeep().addActionListener(this::onAddKnownPlayer);
        trackerPanelUI.getBtnRemovePlayer().addActionListener(this::onRemovePeep);
        trackerPanelUI.getBtnStart().addActionListener(this::onStartSession);
        trackerPanelUI.getBtnStop().addActionListener(this::onStopSession);
        trackerPanelUI.getBtnAddKill().addActionListener(this::onAddKill);
        //        loadHistoryBtn.addActionListener(this::onLoadHistory);
        //        unloadHistoryBtn.addActionListener(this::onUnloadHistory);

        trackerPanelUI.getBtnWaitlistAdd().addActionListener(this::onWaitlistAddSelected);
        trackerPanelUI.getBtnWaitlistDelete().addActionListener(this::onWaitlistDeleteSelected);
        trackerPanelUI.getBtnAddAlt().addActionListener(this::onAddAltToMain);
        trackerPanelUI.getBtnRemoveAlt().addActionListener(this::onRemoveAltFromMain);
        // Refresh alts list when selection changes
        trackerPanelUI.getKnownPlayersDropdown().addItemListener(e -> {
            if (e.getStateChange() == java.awt.event.ItemEvent.SELECTED) {
                refreshAltList();
            }
        });

        refreshAllView();
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
        long amt;
        try {
            amt = val == null ? Long.parseLong(trackerPanelUI.getKillAmountField().getText()) : ((Number) val).longValue();
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

    private void onUnloadHistory() {
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
        int res = javax.swing.JOptionPane.showConfirmDialog(this,
                "Remove '" + selected + "'? This will also unlink any alt relationships.",
                "Confirm removal",
                javax.swing.JOptionPane.YES_NO_OPTION,
                javax.swing.JOptionPane.WARNING_MESSAGE);
        if (res != javax.swing.JOptionPane.YES_OPTION) {
            return;
        }
        if (!manager.removeKnownPlayer(selected)) {
            toast(this,"Not found.");
            return;
        }
        manager.saveToConfig();
        Utils.requestUiRefresh().run();
    }

    private void onAddAltToMain(ActionEvent e) {
        String main = (String) trackerPanelUI.getKnownPlayersDropdown().getSelectedItem();
        String alt = (String) trackerPanelUI.getAddAltDropdown().getSelectedItem();
        if (main == null || alt == null) {
            toast(this, "Select a player and an alt to add.");
            return;
        }
        if (!manager.canLinkAltToMain(alt, main)) {
            toast(this, "Cannot link: either main is an alt, alt already linked, or alt is a main.");
            return;
        }
        if (manager.trySetAltMain(alt, main)) {
            toast(this, String.format("Linked %s → %s", alt, main));
            Utils.requestUiRefresh().run();
        } else {
            toast(this, "Failed to link alt.");
        }
    }

    private void onRemoveAltFromMain(ActionEvent e) {
        String selectedMain = (String) trackerPanelUI.getKnownPlayersDropdown().getSelectedItem();
        String selectedEntry = trackerPanelUI.getAltsList().getSelectedValue();
        if (selectedMain == null || selectedMain.isBlank()) {
            toast(this, "Select a player in Known list.");
            return;
        }
        if (selectedEntry == null || selectedEntry.isBlank()) {
            toast(this, "Select an alt in the list to remove.");
            return;
        }

        // Case 1: Info row like "{player} is an alt of {main}" when the selected player is an alt
        if (selectedEntry.contains(" is an alt of ")) {
            String[] parts = selectedEntry.split(" is an alt of ", 2);
            if (parts.length == 2) {
                String alt = parts[0].trim();
                String main = parts[1].trim();
                if (!manager.isAlt(alt)) {
                    toast(this, alt + " is not linked as an alt.");
                    return;
                }
                int res = JOptionPane.showConfirmDialog(this,
                        "Unlink '" + alt + "' from '" + main + "'?",
                        "Confirm unlink",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                if (res != JOptionPane.YES_OPTION) return;
                if (manager.unlinkAlt(alt)) {
                    toast(this, "Unlinked alt.");
                    Utils.requestUiRefresh().run();
                } else {
                    toast(this, "Failed to unlink alt.");
                }
                return;
            }
        }

        // Case 2: A plain alt name under the selected main's list
        String alt = selectedEntry.trim();
        if (!manager.isAlt(alt)) {
            toast(this, alt + " is not linked as an alt.");
            return;
        }
        String main = manager.getMainName(alt);
        if (main == null || !main.equalsIgnoreCase(selectedMain)) {
            toast(this, String.format("%s is linked to %s, not %s.", alt, main, selectedMain));
            return;
        }
        int res = JOptionPane.showConfirmDialog(this,
                "Unlink '" + alt + "' from '" + main + "'?",
                "Confirm unlink",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (res != JOptionPane.YES_OPTION) return;
        if (manager.unlinkAlt(alt)) {
            toast(this, "Unlinked alt.");
            Utils.requestUiRefresh().run();
        } else {
            toast(this, "Failed to unlink alt.");
        }
    }

    private void onWaitlistAddSelected(ActionEvent e) {
        int idx = trackerPanelUI.getWaitlistTable().getSelectedRow();
        if (idx < 0) {
            toast(this, "Select a detected value first.");
            return;
        }
        if (!manager.hasActiveSession()) {
            toast(this, "Start a session first.");
            return;
        }
        com.example.pksession.model.WaitlistTableModel m = trackerPanelUI.getWaitlistTableModel();
        com.example.pksession.model.PendingValue pv = m.getRow(idx);
        if (pv == null) return;
        String target = pv.getSuggestedPlayer();
        if (target == null || target.isBlank()) {
            toast(this, "Choose a Suggested Player in the table first.");
            return;
        }
        if (manager.applyPendingValueToPlayer(pv.getId(), target)) {
            Utils.requestUiRefresh().run();
        } else {
            toast(this, "Failed to add value. Is the player in the session?");
        }
    }

    private void onWaitlistDeleteSelected(ActionEvent e) {
        int idx = trackerPanelUI.getWaitlistTable().getSelectedRow();
        if (idx < 0) {
            toast(this, "Select a detected value first.");
            return;
        }
        com.example.pksession.model.WaitlistTableModel m = trackerPanelUI.getWaitlistTableModel();
        com.example.pksession.model.PendingValue pv = m.getRow(idx);
        if (pv == null) return;
        if (manager.removePendingValueById(pv.getId())) {
            Utils.requestUiRefresh().run();
        }
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

    void refreshAltList(){
        String[] peeps = manager.getKnownPlayers().toArray(new String[0]);
        refreshAltList(peeps);
    }

    void refreshAltList(String[] peeps){
        // Update known count label
        trackerPanelUI.getKnownListLabel().setText("Known (" + peeps.length + "):");
        // Populate alts list for the currently selected player
        String selectedMain = (String) trackerPanelUI.getKnownPlayersDropdown().getSelectedItem();
        if (selectedMain == null && peeps.length > 0) {
            selectedMain = peeps[0];
            trackerPanelUI.getKnownPlayersDropdown().setSelectedIndex(0);
        }
        // Update alts label text
        String altsText = (selectedMain != null && !selectedMain.isBlank())
                ? (selectedMain + " known alts:")
                : "Known alts:";
        trackerPanelUI.getAltsLabel().setText(altsText);
        DefaultListModel<String> altsModel = (DefaultListModel<String>) trackerPanelUI.getAltsList().getModel();
        altsModel.clear();
        // If the selected player is an alt, show that info as the first item in the list instead of a label
        if (selectedMain != null && manager.isAlt(selectedMain)) {
            String mainName = manager.getMainName(selectedMain);
            if (mainName != null && !mainName.equalsIgnoreCase(selectedMain)) {
                altsModel.addElement(selectedMain + " is an alt of " + mainName);
            }
        }
        if (selectedMain != null) {
            for (String alt : manager.getAltsOf(selectedMain)) {
                altsModel.addElement(alt);
            }
        }
        // Populate add-alt dropdown with eligible candidates
        java.util.List<String> eligible = new java.util.ArrayList<>();
        for (String p : manager.getKnownPlayers()) {
            if (selectedMain == null) break;
            if (manager.canLinkAltToMain(p, selectedMain)) {
                eligible.add(p);
            }
        }
        trackerPanelUI.getAddAltDropdown().setModel(new DefaultComboBoxModel<>(eligible.toArray(new String[0])));

    }


    void refreshAllView() {
        // Update the general dropdown from the Peeps list
        String[] peeps = manager.getKnownPlayers().toArray(new String[0]);

        trackerPanelUI.getKnownPlayersDropdown().setModel(new DefaultComboBoxModel<>(peeps));

        refreshAltList(peeps);

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
            ((Metrics) trackerPanelUI.getMetricsTable().getModel()).setData(manager.computeMetricsFor(current, true));
        }

        // Update recent splits table with the last 10 kills from the current session
        if (current != null) {
            trackerPanelUI.getRecentSplitsModel().setFromKills(current.getKills());
        } else {
            trackerPanelUI.getRecentSplitsModel().clear();
        }

        // Update waitlist table
        com.example.pksession.model.WaitlistTableModel wtm = trackerPanelUI.getWaitlistTableModel();
        java.util.Set<String> mainsOnly = manager.getKnownMains();
        java.util.List<com.example.pksession.model.PendingValue> pvals = manager.getPendingValues();
        wtm.setData(pvals);
        // Update Suggested Player editor (column 2) to show mains only
        javax.swing.JComboBox<String> cb = new javax.swing.JComboBox<>(mainsOnly.toArray(new String[0]));
        trackerPanelUI.getWaitlistTable().getColumnModel().getColumn(2).setCellEditor(new javax.swing.DefaultCellEditor(cb));

        // Refresh dynamic active player buttons
        trackerPanelUI.refreshActivePlayerButtons();

        // Enable/disable based on history
        boolean ro = manager.isHistoryLoaded();
        trackerPanelUI.getBtnStart().setEnabled(!ro && !manager.hasActiveSession());
        trackerPanelUI.getBtnStop().setEnabled(!ro && manager.hasActiveSession());
        trackerPanelUI.getBtnAddToSession().setEnabled(!ro && manager.hasActiveSession());
        trackerPanelUI.getNotInCurrentSessionPlayerDropdown().setEnabled(!ro && manager.hasActiveSession());
        trackerPanelUI.getBtnRemoveFromSession().setEnabled(!ro && manager.hasActiveSession());
        boolean canAddKill = !ro && manager.hasActiveSession();
        trackerPanelUI.getBtnAddKill().setEnabled(canAddKill && trackerPanelUI.getCurrentSessionPlayerDropdown().getItemCount() > 0);
        trackerPanelUI.getKillAmountField().setEnabled(canAddKill && trackerPanelUI.getCurrentSessionPlayerDropdown().getItemCount() > 0);
        // Waitlist buttons enabled if session active and there are rows
        int rows = wtm.getRowCount();
        trackerPanelUI.getBtnWaitlistAdd().setEnabled(!ro && manager.hasActiveSession() && rows > 0);
        trackerPanelUI.getBtnWaitlistDelete().setEnabled(rows > 0);
    }

}