
package com.splitmanager.controllers;

import com.splitmanager.ManagerKnownPlayers;
import com.splitmanager.ManagerPanel;
import com.splitmanager.PluginConfig;
import com.splitmanager.ManagerSession;
import com.splitmanager.models.Metrics;
import com.splitmanager.models.PendingValue;
import com.splitmanager.models.Transfer;
import com.splitmanager.models.Session;
import com.splitmanager.models.WaitlistTable;
import com.splitmanager.views.PanelView;
import com.splitmanager.utils.MarkdownFormatter;
import com.splitmanager.utils.PaymentProcessor;

import javax.swing.*;
import java.util.List;

import static com.splitmanager.utils.Utils.toast;

//Testing push again

/**
 * MVC Controller: non-UI logic + event handling. The View calls into this via PanelActions.
 * Keeps string/markdown/transfer computations here and pushes UI refreshes through the View.
 */
public class PanelController implements PanelActions {
    private final ManagerSession sessionmanager;
    private final PluginConfig config;
    private final PanelView view;
    private final ManagerKnownPlayers playerManager;
    private final ManagerPanel managerPanel;

    public PanelController() {
        sessionmanager = null;
        config = null;
        view = null;
        playerManager = null;
        managerPanel = null;
    }

    public PanelController(ManagerSession sessionManager, PluginConfig config, PanelView view, ManagerKnownPlayers playerManager, ManagerPanel managerPanel) {
        this.sessionmanager = sessionManager;
        this.playerManager = playerManager;
        this.config = config;
        this.view = view;
        this.managerPanel = managerPanel;
    }

    @Override
    public void startSession() {
        if (sessionmanager.isHistoryLoaded()) {
            toast(view, "Unload history first.");
            return;
        }
        if (sessionmanager.hasActiveSession()) {
            toast(view, "Active session exists.");
            return;
        }
        sessionmanager.startSession().ifPresent(s -> toast(view, "Session started."));
        managerPanel.refreshAllView();
        refreshAllView();
    }

    @Override
    public void stopSession() {
        if (sessionmanager.isHistoryLoaded()) {
            toast(view, "Cannot stop while history loaded.");
            return;
        }
        if (sessionmanager.stopSession()) {
            managerPanel.refreshAllView();
            toast(view, "Session stopped.");
        } else {
            toast(view, "Failed to stop session.");
        }
        refreshAllView();
    }

    @Override
    public void addPlayerToSession(String player) {
        if (player == null) {
            toast(view, "Select a player in dropdown.");
            return;
        }
        if (sessionmanager.addPlayerToActive(player)) {
            managerPanel.refreshAllView();
        } else {
            toast(view, "Failed to add player, player might already be in session.");
        }
        refreshAllView();
    }

    @Override
    public void addKnownPlayer(String name) {
        String clean = name == null ? "" : name.trim();
        if (clean.isEmpty()) {
            toast(view, "Enter a name.");
            return;
        }
        if (!playerManager.addKnownPlayer(clean)) {
            toast(view, "Player already in list exists.");
            return;
        }
        playerManager.saveToConfig();
        managerPanel.refreshAllView();
        view.getKnownPlayersDropdown().setSelectedItem(clean);
        view.getNewPeepField().setText("");
        view.getNewPeepField().requestFocusInWindow();
        refreshAllView();
    }

    @Override
    public void removeKnownPlayer(String name) {
        if (name == null) {
            toast(view, "Select a peep to remove.");
            return;
        }
        int res = JOptionPane.showConfirmDialog(view,
                "Remove '" + name + "'? This will also unlink any alt relationships.",
                "Confirm removal",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (res != JOptionPane.YES_OPTION) return;

        if (!playerManager.removeKnownPlayer(name)) {
            toast(view, "Not found.");
            return;
        }
        playerManager.saveToConfig();
        managerPanel.refreshAllView();
        refreshAllView();
    }

    @Override
    public void addKill(String player, long amount) {
        if (player == null) {
            toast(view, "Select a player.");
            return;
        }
        if (sessionmanager.addKill(player, amount)) {
            view.getKillAmountField().setText("");
            managerPanel.refreshAllView();
        } else {
            toast(view, "Failed to add kill (is player in session?).");
        }
        refreshAllView();
    }

    @Override
    public void addAltToMain(String main, String alt) {
        if (main == null || alt == null) {
            toast(view, "Select a player and an alt to add.");
            return;
        }
        if (!playerManager.canLinkAltToMain(alt, main)) {
            toast(view, "Cannot link: either main is an alt, alt already linked, or alt is a main.");
            return;
        }
        if (playerManager.trySetAltMain(alt, main)) {
            toast(view, String.format("Linked %s → %s", alt, main));
            managerPanel.refreshAllView();
            refreshAllView();
        } else {
            toast(view, "Failed to link alt.");
        }
    }

    @Override
    public void removeSelectedAlt(String selectedMain, String selectedEntry) {
        //TODO TEST me, selectedEntry should be a alt?
        if (selectedMain == null || selectedMain.isBlank()) {
            toast(view, "Select a player in Known list.");
            return ;
        }
        if (selectedEntry == null || selectedEntry.isBlank()) {
            toast(view, "Select an alt in the list to remove.");
            return ;
        }

        String alt = playerManager.parseAltFromEntry(selectedEntry);
        String main = playerManager.getMainName(alt);

        if (!playerManager.isAlt(alt)) {
            toast(view, alt + " is not linked as an alt.");
            return ;
        }

        if (main == null || !main.equalsIgnoreCase(selectedMain)) {
            toast(view, String.format("%s is linked to %s, not %s.", alt, main, selectedMain));
            return ;
        }

        int res = JOptionPane.showConfirmDialog(view,
                "Unlink '" + alt + "' from '" + main + "'?",
                "Confirm unlink",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (res != JOptionPane.YES_OPTION) return ;

        if (playerManager.unlinkAlt(alt)) {
            toast(view, "Unlinked alt.");
            managerPanel.refreshAllView();
            playerManager.saveToConfig();
            refreshAllView();
        } else {
            toast(view, "Failed to unlink alt.");
        }
    }

    @Override
    public void applySelectedPendingValue(int idx) {
        if (idx < 0) {
            toast(view, "Select a detected value first.");
            return;
        }
        if (!sessionmanager.hasActiveSession()) {
            toast(view, "Start a session first.");
            return;
        }
        WaitlistTable m = view.getWaitlistTableModel();
        PendingValue pv = m.getRow(idx);
        if (pv == null) return;
        String target = pv.getSuggestedPlayer();
        if (target == null || target.isBlank()) {
            toast(view, "Choose a Suggested Player in the table first.");
            return;
        }
        if (sessionmanager.applyPendingValueToPlayer(pv.getId(), target)) {
            managerPanel.refreshAllView();
        } else {
            toast(view, "Failed to add value. Is the player in the session?");
        }
        refreshAllView();
    }

    @Override
    public void deleteSelectedPendingValue(int idx) {
        if (idx < 0) {
            toast(view, "Select a detected value first.");
            return;
        }
        WaitlistTable m = view.getWaitlistTableModel();
        PendingValue pv = m.getRow(idx);
        if (pv == null) return;
        if (sessionmanager.removePendingValueById(pv.getId())) {
            managerPanel.refreshAllView();
        }
        refreshAllView();
    }

    @Override
    public void onKnownPlayerSelectionChanged(String selected) {
        refreshAlts();
    }

    @Override
    public void refreshAllView() {
        refreshKnownPlayers();
        refreshSessionData();
        refreshWaitlist();
        refreshButtonStates();
    }

    /**
     * Refreshes the list of known players and updates corresponding UI components.
     * <p>
     * This method retrieves the list of known players from the session manager and updates
     * the dropdown menu for known players in the user interface, as well as the label showing
     * the count of known players. It ensures synchronization between the backend data and
     * the visual presentation of known players.
     */
    private void refreshKnownPlayers() {
        String[] players = sessionmanager.getKnownPlayers().toArray(new String[0]);
        view.getKnownPlayersDropdown().setModel(new DefaultComboBoxModel<>(players));
        view.getKnownListLabel().setText("Known (" + players.length + "):");

        refreshAlts();
    }

    /**
     * Refreshes and updates user interface components related to the current session data.
     */
    private void refreshSessionData() {
        Session currentSession = sessionmanager.getCurrentSession().orElse(null);

        if (currentSession != null && currentSession.isActive()) {
            String[] sessionPlayers = currentSession.getPlayers().toArray(new String[0]);
            String[] notPeeps = sessionmanager.getNonActivePlayers().toArray(new String[0]);

            view.getCurrentSessionPlayerDropdown().setEnabled(true);
            view.getCurrentSessionPlayerDropdown().setModel(new DefaultComboBoxModel<>(sessionPlayers));
            view.getNotInCurrentSessionPlayerDropdown().setModel(new DefaultComboBoxModel<>(notPeeps));
        } else {
            view.getCurrentSessionPlayerDropdown().setModel(new DefaultComboBoxModel<>(new String[0]));
            view.getCurrentSessionPlayerDropdown().setEnabled(false);
        }

        view.getHistoryLabel().setText("History: " + (sessionmanager.isHistoryLoaded() ? "ON" : "OFF"));

        Session current = sessionmanager.getCurrentSession().orElse(null);
        if (current != null) {
            ((Metrics) view.getMetricsTable().getModel())
                    .setData(sessionmanager.computeMetricsFor(current, true));
            view.getRecentSplitsModel().setFromKills(current.getKills());
        } else {
            view.getRecentSplitsModel().clear();
        }
    }

    /**
     * Refreshes the data and UI components related to the waitlist table.
     * <p>
     * This method fetches the latest pending values from the session manager and updates
     * the waitlist table model with this data. It also configures the cell editor for the
     * third column of the waitlist table, populating it with a dropdown of known main players
     * retrieved from the session manager.
     * <p>
     * Ensures that the waitlist table reflects the most up-to-date state of the application.
     */
    private void refreshWaitlist() {
        view.getWaitlistTableModel().setData(sessionmanager.getPendingValues());
        view.getWaitlistTable()
                .getColumnModel()
                .getColumn(2)
                .setCellEditor(new DefaultCellEditor(new JComboBox<>(
                        playerManager.getKnownMains().toArray(new String[0]
                        )
                )));
    }

    /**
     * Updates the enabled or disabled state of various buttons and fields in the user interface.
     * The button states are set based on the current session status, player selections, and
     * whether the session is in a read-only mode.
     */
    private void refreshButtonStates() {
        view.refreshActivePlayerButtons();

        boolean readOnly = sessionmanager.isHistoryLoaded();
        boolean hasActiveSession = sessionmanager.hasActiveSession();

        view.getBtnStart().setEnabled(!readOnly && !hasActiveSession);
        view.getBtnStop().setEnabled(!readOnly && hasActiveSession);
        view.getBtnAddToSession().setEnabled(!readOnly && hasActiveSession);
        view.getNotInCurrentSessionPlayerDropdown().setEnabled(!readOnly && hasActiveSession);
        view.getBtnRemoveFromSession().setEnabled(!readOnly && hasActiveSession);

        boolean canAddKill = !readOnly && hasActiveSession;
        boolean hasSessionPlayers = view.getCurrentSessionPlayerDropdown().getItemCount() > 0;

        view.getBtnAddKill().setEnabled(canAddKill && hasSessionPlayers);
        view.getKillAmountField().setEnabled(canAddKill && hasSessionPlayers);

        int waitlistRows = view.getWaitlistTableModel().getRowCount();

        view.getBtnWaitlistAdd().setEnabled(!readOnly && hasActiveSession && waitlistRows > 0);
        view.getBtnWaitlistDelete().setEnabled(waitlistRows > 0);
    }

    /**
     * Refreshes the dropdown lists and UI components related to alternate accounts.
     * This method ensures that the UI reflects the currently selected main player
     * and dynamically updates the list of eligible alternate accounts that can be linked to the selected player.
     */
    private void refreshAlts() {
        String[] players = sessionmanager.getKnownPlayers().toArray(new String[0]);

        String selectedMain = (String) view.getKnownPlayersDropdown().getSelectedItem();
        if (selectedMain == null && players.length > 0) {
            selectedMain = players[0];
            view.getKnownPlayersDropdown().setSelectedIndex(0);
        }

        refreshAltList(selectedMain);

        List<String> eligiblePlayers = new java.util.ArrayList<>();

        if (selectedMain != null) {
            for (String p : sessionmanager.getKnownPlayers()) {
                if (playerManager.canLinkAltToMain(p, selectedMain)) {
                    eligiblePlayers.add(p);
                }
            }
        }

        view.getAddAltDropdown().setModel(new DefaultComboBoxModel<>(eligiblePlayers.toArray(new String[0])));
    }

    /**
     * Refreshes the list of alternate accounts associated with the given main player.
     * Updates the alt label and list in the view with relevant information about the player's alt accounts.
     * If the player is identified as an alt, displays the corresponding main account name.
     *
     * @param mainPlayer the name of the main player whose alternate accounts are to be refreshed;
     *                   if null, the method exits without performing any action.
     */
    public void refreshAltList(String mainPlayer) {
        if (mainPlayer == null) return;

        String altsText = !mainPlayer.isBlank()
                ? (mainPlayer + " known alts:")
                : "Known alts:";
        view.getAltsLabel().setText(altsText);

        DefaultListModel<String> altsModel = (DefaultListModel<String>) view.getAltsList().getModel();
        altsModel.clear();

        if (playerManager.isAlt(mainPlayer)) {
            String mainName = playerManager.getMainName(mainPlayer);
            if (mainName != null && !mainName.equalsIgnoreCase(mainPlayer)) {
                altsModel.addElement(mainPlayer + " is an alt of " + mainName);
            }
        }

        for (String alt : playerManager.getAltsOf(mainPlayer)) {
            altsModel.addElement(alt);
        }
    }

    /**
     * Builds a JSON representation of the player metrics for the current session.
     * The method retrieves the current session and computes metrics for each player in that session.
     * The metrics are then formatted as a JSON array, where each entry contains the player's name,
     * total count, split count, and active player status.
     *
     * @return A JSON string representing the computed player metrics for the current session.
     * Returns an empty JSON array "[]" if no session is active or there are no metrics.
     */
    public String buildMetricsJson() {
        Session currentSession = sessionmanager.getCurrentSession().orElse(null);
        List<ManagerSession.PlayerMetrics> data = sessionmanager.computeMetricsFor(currentSession, true);
        StringBuilder sb = new StringBuilder();

        sb.append("[");
        for (int i = 0; i < data.size(); i++) {
            ManagerSession.PlayerMetrics pm = data.get(i);
            sb.append("{\"player\":\"").append(pm.player).append("\",")
                    .append("\"total\":").append(pm.total).append(",")
                    .append("\"split\":").append(pm.split).append(",")
                    .append("\"active\":").append(pm.activePlayer).append("}");
            if (i < data.size() - 1) sb.append(",");
        }
        sb.append("]");

        return sb.toString();
    }

    public String buildMetricsMarkdown() {
        List<ManagerSession.PlayerMetrics> data =
                sessionmanager.computeMetricsFor(sessionmanager.getCurrentSession().orElse(null), true);
        return MarkdownFormatter.buildMetricsMarkdown(data, config);
    }

    public List<String> computeDirectPayments(List<ManagerSession.PlayerMetrics> data) {
        return PaymentProcessor.computeDirectPayments(data);
    }

    public List<Transfer> computeDirectPaymentsStructured(List<ManagerSession.PlayerMetrics> data) {
        return PaymentProcessor.computeDirectPaymentsStructured(data);
    }
}