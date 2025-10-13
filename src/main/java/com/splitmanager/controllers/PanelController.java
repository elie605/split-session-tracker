package com.splitmanager.controllers;

import com.splitmanager.utils.Formats;
import com.splitmanager.PluginConfig;
import com.splitmanager.ManagerSession;
import com.splitmanager.models.Metrics;
import com.splitmanager.models.PendingValue;
import com.splitmanager.models.Transfer;
import com.splitmanager.models.Session;
import com.splitmanager.models.WaitlistTable;
import com.splitmanager.views.PanelView;
import com.splitmanager.utils.Utils;

import javax.swing.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import static com.splitmanager.utils.Utils.toast;

/**
 * MVC Controller: non-UI logic + event handling. The View calls into this via PanelActions.
 * Keeps string/markdown/transfer computations here and pushes UI refreshes through the View.
 */
public class PanelController implements PanelActions {
    private final ManagerSession sessionmanager;
    private final PluginConfig config;
    private final PanelView view;

    public PanelController(ManagerSession sessionmanager, PluginConfig config, PanelView view) {
        this.sessionmanager = sessionmanager;
        this.config = config;
        this.view = view;
    }

    // ---------- Actions from the View ----------

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
        Utils.requestUiRefresh().run();
        refreshAllView();
    }

    @Override
    public void stopSession() {
        if (sessionmanager.isHistoryLoaded()) {
            toast(view, "Cannot stop while history loaded.");
            return;
        }
        if (sessionmanager.stopSession()) {
            Utils.requestUiRefresh().run();
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
            Utils.requestUiRefresh().run();
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
        if (!sessionmanager.addKnownPlayer(clean)) {
            toast(view, "Player already in list exists.");
            return;
        }
        sessionmanager.saveToConfig();
        Utils.requestUiRefresh().run();
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

        if (!sessionmanager.removeKnownPlayer(name)) {
            toast(view, "Not found.");
            return;
        }
        sessionmanager.saveToConfig();
        Utils.requestUiRefresh().run();
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
            Utils.requestUiRefresh().run();
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
        if (!sessionmanager.canLinkAltToMain(alt, main)) {
            toast(view, "Cannot link: either main is an alt, alt already linked, or alt is a main.");
            return;
        }
        if (sessionmanager.trySetAltMain(alt, main)) {
            toast(view, String.format("Linked %s → %s", alt, main));
            Utils.requestUiRefresh().run();
        } else {
            toast(view, "Failed to link alt.");
        }
        refreshAllView();
    }

    @Override
    public void removeSelectedAlt(String selectedMain, String selectedEntry) {
        if (selectedMain == null || selectedMain.isBlank()) {
            toast(view, "Select a player in Known list.");
            return;
        }
        if (selectedEntry == null || selectedEntry.isBlank()) {
            toast(view, "Select an alt in the list to remove.");
            return;
        }

        if (selectedEntry.contains(" is an alt of ")) {
            String[] parts = selectedEntry.split(" is an alt of ", 2);
            if (parts.length == 2) {
                String alt = parts[0].trim();
                String main = parts[1].trim();
                if (!sessionmanager.isAlt(alt)) {
                    toast(view, alt + " is not linked as an alt.");
                    return;
                }
                int res = JOptionPane.showConfirmDialog(view,
                        "Unlink '" + alt + "' from '" + main + "'?",
                        "Confirm unlink",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                if (res != JOptionPane.YES_OPTION) return;
                if (sessionmanager.unlinkAlt(alt)) {
                    toast(view, "Unlinked alt.");
                    Utils.requestUiRefresh().run();
                } else {
                    toast(view, "Failed to unlink alt.");
                }
            }
            refreshAllView();
            return;
        }

        String alt = selectedEntry.trim();
        if (!sessionmanager.isAlt(alt)) {
            toast(view, alt + " is not linked as an alt.");
            return;
        }
        String main = sessionmanager.getMainName(alt);
        if (main == null || !main.equalsIgnoreCase(selectedMain)) {
            toast(view, String.format("%s is linked to %s, not %s.", alt, main, selectedMain));
            return;
        }
        int res = JOptionPane.showConfirmDialog(view,
                "Unlink '" + alt + "' from '" + main + "'?",
                "Confirm unlink",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (res != JOptionPane.YES_OPTION) return;
        if (sessionmanager.unlinkAlt(alt)) {
            toast(view, "Unlinked alt.");
            Utils.requestUiRefresh().run();
        } else {
            toast(view, "Failed to unlink alt.");
        }
        refreshAllView();
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
            Utils.requestUiRefresh().run();
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
            Utils.requestUiRefresh().run();
        }
        refreshAllView();
    }

    @Override
    public void onKnownPlayerSelectionChanged(String selected) {
        refreshAltList();
    }

    //TODO Refactor and Split this up to different functions so it can be called more efficiently
    @Override
    public void refreshAllView() {
        // Update Known list + labels
        String[] peeps = sessionmanager.getKnownPlayers().toArray(new String[0]);
        view.getKnownPlayersDropdown().setModel(new DefaultComboBoxModel<>(peeps));
        refreshAltList(peeps);

        // Session players dropdowns
        Session currentSession = sessionmanager.getCurrentSession().orElse(null);
        if (currentSession != null && currentSession.isActive()) {
            String[] sessionPlayers = currentSession.getPlayers().toArray(new String[0]);
            String[] notPeeps = sessionmanager.getNonActivePlayers().toArray(new String[0]);
            view.getCurrentSessionPlayerDropdown().setModel(new DefaultComboBoxModel<>(sessionPlayers));
            view.getCurrentSessionPlayerDropdown().setEnabled(true);
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

        // Waitlist
        var wtm = view.getWaitlistTableModel();
        java.util.Set<String> mainsOnly = sessionmanager.getKnownMains();
        java.util.List<PendingValue> pvals = sessionmanager.getPendingValues();
        wtm.setData(pvals);
        javax.swing.JComboBox<String> cb = new javax.swing.JComboBox<>(mainsOnly.toArray(new String[0]));
        view.getWaitlistTable().getColumnModel().getColumn(2).setCellEditor(new javax.swing.DefaultCellEditor(cb));

        // Dynamic buttons
        view.refreshActivePlayerButtons();

        // Enable/disable toggles
        boolean ro = sessionmanager.isHistoryLoaded();
        view.getBtnStart().setEnabled(!ro && !sessionmanager.hasActiveSession());
        view.getBtnStop().setEnabled(!ro && sessionmanager.hasActiveSession());
        view.getBtnAddToSession().setEnabled(!ro && sessionmanager.hasActiveSession());
        view.getNotInCurrentSessionPlayerDropdown().setEnabled(!ro && sessionmanager.hasActiveSession());
        view.getBtnRemoveFromSession().setEnabled(!ro && sessionmanager.hasActiveSession());
        boolean canAddKill = !ro && sessionmanager.hasActiveSession();
        view.getBtnAddKill().setEnabled(canAddKill && view.getCurrentSessionPlayerDropdown().getItemCount() > 0);
        view.getKillAmountField().setEnabled(canAddKill && view.getCurrentSessionPlayerDropdown().getItemCount() > 0);
        int rows = wtm.getRowCount();
        view.getBtnWaitlistAdd().setEnabled(!ro && sessionmanager.hasActiveSession() && rows > 0);
        view.getBtnWaitlistDelete().setEnabled(rows > 0);
    }

    private void refreshAltList() {
        refreshAltList(sessionmanager.getKnownPlayers().toArray(new String[0]));
    }

    private void refreshAltList(String[] peeps) {
        view.getKnownListLabel().setText("Known (" + peeps.length + "):");
        String selectedMain = (String) view.getKnownPlayersDropdown().getSelectedItem();
        if (selectedMain == null && peeps.length > 0) {
            selectedMain = peeps[0];
            view.getKnownPlayersDropdown().setSelectedIndex(0);
        }
        String altsText = (selectedMain != null && !selectedMain.isBlank())
                ? (selectedMain + " known alts:")
                : "Known alts:";
        view.getAltsLabel().setText(altsText);
        DefaultListModel<String> altsModel = (DefaultListModel<String>) view.getAltsList().getModel();
        altsModel.clear();
        if (selectedMain != null && sessionmanager.isAlt(selectedMain)) {
            String mainName = sessionmanager.getMainName(selectedMain);
            if (mainName != null && !mainName.equalsIgnoreCase(selectedMain)) {
                altsModel.addElement(selectedMain + " is an alt of " + mainName);
            }
        }
        if (selectedMain != null) {
            for (String alt : sessionmanager.getAltsOf(selectedMain)) altsModel.addElement(alt);
        }
        java.util.List<String> eligible = new java.util.ArrayList<>();
        for (String p : sessionmanager.getKnownPlayers()) {
            if (selectedMain == null) break;
            if (sessionmanager.canLinkAltToMain(p, selectedMain)) eligible.add(p);
        }
        view.getAddAltDropdown().setModel(new DefaultComboBoxModel<>(eligible.toArray(new String[0])));
    }

    // ---------- Existing non-UI helpers (kept) ----------

    public String buildMetricsJson() { /* unchanged from original */
        var currentSession = sessionmanager.getCurrentSession().orElse(null);
        List<ManagerSession.PlayerMetrics> data = sessionmanager.computeMetricsFor(currentSession, true);
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < data.size(); i++) {
            var pm = data.get(i);
            sb.append("{\"player\":\"").append(pm.player).append("\",")
                    .append("\"total\":").append(pm.total).append(",")
                    .append("\"split\":").append(pm.split).append(",")
                    .append("\"active\":").append(pm.activePlayer).append("}");
            if (i < data.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    public String buildMetricsMarkdown() { /* unchanged core with direct/middleman logic */
        var currentSession = sessionmanager.getCurrentSession().orElse(null);
        List<ManagerSession.PlayerMetrics> data = sessionmanager.computeMetricsFor(currentSession, true);
        DecimalFormat df = Formats.getDecimalFormat();

        boolean directMode = config.directPayments();
        boolean forDiscord = config.copyForDiscord();

        StringBuilder sb = new StringBuilder();
        if (forDiscord) sb.append("```\n");

        if (!directMode) {
            List<String[]> rows = new ArrayList<>();
            int maxPlayer = "Player".length();
            int maxTotal = "Total".length();
            int maxSplit = "Split".length();
            for (var pm : data) {
                String player = pm.player == null ? "" : pm.player.replace("|", "\\|");
                String total = df.format(pm.total);
                long dispSplit = pm.split;
                if (config.flipSettlementSign()) dispSplit = -dispSplit;
                String split = df.format(dispSplit);
                rows.add(new String[]{player, total, split});
                if (player.length() > maxPlayer) maxPlayer = player.length();
                if (total.length() > maxTotal) maxTotal = total.length();
                if (split.length() > maxSplit) maxSplit = split.length();
            }
            sb.append("| ").append(padRight("Player", maxPlayer)).append(" | ")
                    .append(padLeft("Total", maxTotal)).append(" | ")
                    .append(padLeft("Split", maxSplit)).append(" |\n");
            sb.append("| ").append(repeat('-', maxPlayer)).append(" | ")
                    .append(repeat('-', maxTotal - 1)).append(":").append(" | ")
                    .append(repeat('-', maxSplit - 1)).append(":").append(" |\n");
            for (String[] r : rows) {
                sb.append("| ").append(padRight(r[0], maxPlayer)).append(" | ")
                        .append(padLeft(r[1], maxTotal)).append(" | ")
                        .append(padLeft(r[2], maxSplit)).append(" |\n");
            }
        } else {
            List<String[]> rows = new ArrayList<>();
            int maxPlayer = "Player".length();
            int maxSplit = "Split".length();
            for (var pm : data) {
                String player = pm.player == null ? "" : pm.player.replace("|", "\\|");
                long dispSplit = pm.split;
                String split = df.format(dispSplit);
                rows.add(new String[]{player, split});
                if (player.length() > maxPlayer) maxPlayer = player.length();
                if (split.length() > maxSplit) maxSplit = split.length();
            }
            sb.append("| ").append(padRight("Player", maxPlayer)).append(" | ")
                    .append(padLeft("Split", maxSplit)).append(" |\n");
            sb.append("| ").append(repeat('-', maxPlayer)).append(" | ")
                    .append(repeat('-', maxSplit - 1)).append(":").append(" |\n");
            for (String[] r : rows) {
                sb.append("| ").append(padRight(r[0], maxPlayer)).append(" | ")
                        .append(padLeft(r[1], maxSplit)).append(" |\n");
            }
        }

        if (config.directPayments()) {
            List<String> transfers = computeDirectPayments(data);
            if (!transfers.isEmpty()) {
                sb.append('\n').append("Suggested direct payments:\n");
                for (String line : transfers) sb.append("- ").append(line).append('\n');
            }
        }

        if (forDiscord) sb.append("```\n");
        return sb.toString();
    }

    public List<String> computeDirectPayments(List<ManagerSession.PlayerMetrics> data) {
        List<ManagerSession.PlayerMetrics> receivers = new ArrayList<>();
        List<ManagerSession.PlayerMetrics> payers = new ArrayList<>();
        for (var pm : data) {
            if (pm.split > 0) receivers.add(pm);
            else if (pm.split < 0) payers.add(pm);
        }
        receivers.sort((a, b) -> Long.compare(b.split, a.split));
        payers.sort((a, b) -> Long.compare(Math.abs(b.split), Math.abs(a.split)));

        List<String> lines = new ArrayList<>();
        DecimalFormat df = Formats.getDecimalFormat();

        int i = 0, j = 0;
        long recvLeft = receivers.isEmpty() ? 0 : receivers.get(0).split;
        long payLeft = payers.isEmpty() ? 0 : -payers.get(0).split;
        while (i < receivers.size() && j < payers.size()) {
            long amt = Math.min(recvLeft, payLeft);
            if (amt > 0) {
                String from = payers.get(j).player;
                String to = receivers.get(i).player;
                lines.add(from + " -> " + to + ": " + df.format(amt));
                recvLeft -= amt;
                payLeft -= amt;
            }
            if (recvLeft == 0) {
                i++;
                if (i < receivers.size()) recvLeft = receivers.get(i).split;
            }
            if (payLeft == 0) {
                j++;
                if (j < payers.size()) payLeft = -payers.get(j).split;
            }
        }
        return lines;
    }

    public List<Transfer> computeDirectPaymentsStructured(List<ManagerSession.PlayerMetrics> data) {
        List<ManagerSession.PlayerMetrics> receivers = new ArrayList<>();
        List<ManagerSession.PlayerMetrics> payers = new ArrayList<>();
        for (ManagerSession.PlayerMetrics pm : data) {
            if (pm.split > 0) receivers.add(pm);
            else if (pm.split < 0) payers.add(pm);
        }
        receivers.sort((a, b) -> Long.compare(b.split, a.split));
        payers.sort((a, b) -> Long.compare(Math.abs(b.split), Math.abs(a.split)));

        List<Transfer> out = new ArrayList<>();
        int i = 0, j = 0;
        long recvLeft = receivers.isEmpty() ? 0 : receivers.get(0).split;
        long payLeft = payers.isEmpty() ? 0 : -payers.get(0).split;

        while (i < receivers.size() && j < payers.size()) {
            long amt = Math.min(recvLeft, payLeft);
            if (amt > 0) {
                String from = payers.get(j).player;
                String to = receivers.get(i).player;
                out.add(new Transfer(from, to, amt));
                recvLeft -= amt;
                payLeft -= amt;
            }
            if (recvLeft == 0) {
                i++;
                if (i < receivers.size()) recvLeft = receivers.get(i).split;
            }
            if (payLeft == 0) {
                j++;
                if (j < payers.size()) payLeft = -payers.get(j).split;
            }
        }
        return out;
    }

    private static String padRight(String s, int width) {
        if (s == null) s = "";
        if (s.length() >= width) return s;
        StringBuilder sb = new StringBuilder(width);
        sb.append(s);
        for (int i = s.length(); i < width; i++) sb.append(' ');
        return sb.toString();
    }

    private static String padLeft(String s, int width) {
        if (s == null) s = "";
        StringBuilder sb = new StringBuilder(width);
        for (int i = s.length(); i < width; i++) sb.append(' ');
        sb.append(s);
        return sb.toString();
    }

    private static String repeat(char ch, int count) {
        if (count <= 0) return "";
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) sb.append(ch);
        return sb.toString();
    }
}
