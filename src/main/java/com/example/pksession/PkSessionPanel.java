package com.example.pksession;

import com.example.pksession.model.Session;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.text.DecimalFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;

import net.runelite.client.ui.PluginPanel;

public class PkSessionPanel extends PluginPanel {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());
    private static final DecimalFormat DF = new DecimalFormat("#,##0.00");

    private final com.example.pksession.SessionManager manager;
    private final PkSessionConfig config;
    private final Runnable refreshUi;

    // Peeps / Player selection (single dropdown used everywhere)
    private final JComboBox<String> peepDropdown = new JComboBox<>();
    private final JTextField newPeepField = new JTextField();

    // Session controls
    private final JButton startBtn = new JButton("Start");
    private final JButton stopBtn = new JButton("Stop");
    private final JLabel historyLabel = new JLabel("History: OFF");

    // Roster controls (use peepDropdown to choose a known player to add/remove to current session)
    private final JButton addToSessionBtn = new JButton("Add player to session");
    private final JButton removeFromSessionBtn = new JButton("Remove player from session");

    // Kill controls (uses the same peepDropdown)
    private final JFormattedTextField killAmountField = new JFormattedTextField();
    private final JButton addKillBtn = new JButton("Add kill/value");

    // Live table
    private final JTable metricsTable = new JTable(new MetricsModel());

    // History list
    private final DefaultListModel<Session> historyModel = new DefaultListModel<>();
    private final JList<Session> historyList = new JList<>(historyModel);


    public PkSessionPanel(SessionManager manager, PkSessionConfig config, Runnable refreshUi) {
        this.manager = manager;
        this.config = config;
        this.refreshUi = refreshUi;

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel top = new JPanel();
        top.setLayout(new javax.swing.BoxLayout(top, javax.swing.BoxLayout.Y_AXIS));

        // Kill/Value - move to top, use single dropdown for player
        JPanel killPanel = new JPanel();
        killPanel.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createTitledBorder("Kill/Value"),
                        BorderFactory.createEmptyBorder(8, 8, 48, 8)
                )
        );
        killAmountField.setColumns(8);
        peepDropdown.setPreferredSize(new Dimension(180, 24));
        killPanel.add(new JLabel("Player:"));
        killPanel.add(peepDropdown);
        killPanel.add(new JLabel("Amount:"));
        killPanel.add(killAmountField);
        killPanel.add(addKillBtn);

        // Peeps (Known players)
        JPanel peepsPanel = new JPanel();
        peepsPanel.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createTitledBorder("Players"),
                        BorderFactory.createEmptyBorder(8, 8, 96, 8)
                )
        );
        peepDropdown.setPreferredSize(new Dimension(128, 24));

        // Ensure the input is editable and visible
        newPeepField.setEditable(true);
        newPeepField.setFocusable(true);
        newPeepField.setColumns(14);
        newPeepField.setPreferredSize(new Dimension(135, 24));
        newPeepField.setToolTipText("Type a name and click 'Add peep'");
        newPeepField.setOpaque(true);

        JPanel pListRowPanel = new JPanel();
        pListRowPanel.add(new JLabel("List:"));
        pListRowPanel.add(peepDropdown);
        JPanel nListRowPanel = new JPanel();
        nListRowPanel.add(new JLabel("Name:"));
        nListRowPanel.add(newPeepField);

        peepsPanel.add(nListRowPanel);
        peepsPanel.add(pListRowPanel);

        JButton addPeepBtn = new JButton("Add peep");
        JButton removePeepBtn = new JButton("Remove peep");
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(addPeepBtn);
        buttonPanel.add(removePeepBtn);
        peepsPanel.add(buttonPanel);
        // Session controls
        JPanel sessionPanel = new JPanel();
        sessionPanel.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createTitledBorder("Session"),
                        BorderFactory.createEmptyBorder(8, 8, 48, 8)
                )
        );

        JPanel buttonRow = new JPanel();
        buttonRow.add(startBtn);
        buttonRow.add(stopBtn);
        sessionPanel.add(buttonRow);
        sessionPanel.add(historyLabel);

        // Roster
        JPanel rosterPanel = new JPanel();
        rosterPanel.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createTitledBorder("Roster"),
                        BorderFactory.createEmptyBorder(8, 8, 48, 8)
                )
        );
        rosterPanel.add(addToSessionBtn);
        rosterPanel.add(removeFromSessionBtn);

        // Order: Kill/Value at top, then Peeps, Session, Roster
        top.add(killPanel);
        top.add(Box.createVerticalStrut(8));
        top.add(peepsPanel);
        top.add(Box.createVerticalStrut(8));
        top.add(sessionPanel);
        top.add(Box.createVerticalStrut(8));
        top.add(rosterPanel);

        // Metrics table
        metricsTable.setFillsViewportHeight(true);
        JScrollPane tableScroll = new JScrollPane(metricsTable);
        tableScroll.setPreferredSize(new Dimension(320, 180));
        tableScroll.setBorder(
                BorderFactory.createCompoundBorder(
                        tableScroll.getBorder(),
                        BorderFactory.createEmptyBorder(6, 6, 64, 6)
                )
        );

        // History
        JPanel historyPanel = new JPanel();
        historyPanel.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createTitledBorder("Past Sessions"),
                        BorderFactory.createEmptyBorder(8, 8, 64, 8)
                )
        );
        historyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        historyList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel lbl = new JLabel(sessionLabel(value));
            if (isSelected) {
                lbl.setOpaque(true);
            }
            return lbl;
        });
        JScrollPane histScroll = new JScrollPane(historyList);
        histScroll.setPreferredSize(new Dimension(320, 160));
        histScroll.setBorder(
                BorderFactory.createCompoundBorder(
                        histScroll.getBorder(),
                        BorderFactory.createEmptyBorder(6, 6, 64, 6)
                )
        );
        historyPanel.add(histScroll);
        JButton loadHistoryBtn = new JButton("Load selected");
        historyPanel.add(loadHistoryBtn);
        JButton unloadHistoryBtn = new JButton("Unload history");
        historyPanel.add(unloadHistoryBtn);

        JPanel center = new JPanel();
        center.setLayout(new javax.swing.BoxLayout(center, javax.swing.BoxLayout.Y_AXIS));
        center.add(tableScroll);
        center.add(Box.createVerticalStrut(8));
        center.add(historyPanel);

        add(top, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);

        // Actions
        addPeepBtn.addActionListener(this::onAddPeep);
        removePeepBtn.addActionListener(this::onRemovePeep);
        startBtn.addActionListener(this::onStartSession);
        stopBtn.addActionListener(this::onStopSession);
        addToSessionBtn.addActionListener(this::onAddToSession);
        removeFromSessionBtn.addActionListener(this::onRemoveFromSession);
        addKillBtn.addActionListener(this::onAddKill);
        loadHistoryBtn.addActionListener(this::onLoadHistory);
        unloadHistoryBtn.addActionListener(this::onUnloadHistory);

        refresh();
    }



    private void onStartSession(ActionEvent e) {
        if (manager.isHistoryLoaded()) {
            toast("Unload history first.");
            return;
        }
        if (manager.hasActiveSession()) {
            toast("Active session exists.");
            return;
        }
        manager.startSession().ifPresent(s -> toast("Session started."));
        refreshUi.run();
    }

    private void onStopSession(ActionEvent e) {
        if (manager.isHistoryLoaded()) {
            toast("Cannot stop while history loaded.");
            return;
        }
        if (!manager.stopSession()) {
            toast("No active session.");
            return;
        }
        toast("Session stopped.");
        refreshUi.run();
    }

    private void onAddToSession(ActionEvent e) {
        String player = (String) peepDropdown.getSelectedItem();
        if (player == null) {
            toast("Select a player in dropdown.");
            return;
        }
        if (manager.addPlayerToActive(player)) {
            refreshUi.run();
        } else {
            toast("Failed to add player.");
        }
    }

    private void onRemoveFromSession(ActionEvent e) {
        String player = (String) peepDropdown.getSelectedItem();
        if (player == null) {
            toast("Select a player in dropdown.");
            return;
        }
        if (manager.removePlayerFromActive(player)) {
            toast("Player removed.");
            refreshUi.run();
        } else {
            toast("Failed to remove player.");
        }
    }

    private void onAddKill(ActionEvent e) {
        String player = (String) peepDropdown.getSelectedItem();
        if (player == null) {
            toast("Select a player.");
            return;
        }
        Object val = killAmountField.getValue();
        double amt;
        try {
            amt = val == null ? Double.parseDouble(killAmountField.getText()) : ((Number) val).doubleValue();
        } catch (Exception ex) {
            toast("Invalid amount.");
            return;
        }
        if (manager.addKill(player, amt)) {
            toast("Kill added.");
            refreshUi.run();
        } else {
            toast("Failed to add kill (is player in session?).");
        }
    }

    private void onLoadHistory(ActionEvent e) {
        Session s = historyList.getSelectedValue();
        if (s == null) {
            toast("Select a session to load.");
            return;
        }
        if (manager.hasActiveSession()) {
            toast("Stop active session first.");
            return;
        }
        Optional<Session> loaded = manager.loadHistory(s.getId());
        toast(loaded.isPresent() ? "History loaded." : "Failed to load history.");
        refreshUi.run();
    }

    private void onUnloadHistory(ActionEvent e) {
        if (!manager.isHistoryLoaded()) {
            toast("Nothing to unload.");
            return;
        }
        manager.unloadHistory();
        toast("History unloaded.");
        refreshUi.run();
    }

    private void toast(String msg) {
        JOptionPane.showMessageDialog(this, msg);
    }

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

    void refresh() {
        // Update the single dropdown from the Peeps list
        String[] peeps = manager.getPeeps().toArray(new String[0]);
        peepDropdown.setModel(new DefaultComboBoxModel<>(peeps));

        // History list
//        historyModel.clear();
//        manager.getAllSessionsNewestFirst().forEach(historyModel::addElement);

        // History label
        historyLabel.setText("History: " + (manager.isHistoryLoaded() ? "ON" : "OFF"));

        // Table data
        Session current = manager.getCurrentSession().orElse(null);
        ((MetricsModel) metricsTable.getModel()).setData(current, manager.computeMetricsFor(current));

        // Enable/disable based on history
        boolean ro = manager.isHistoryLoaded();
        startBtn.setEnabled(!ro && !manager.hasActiveSession());
        stopBtn.setEnabled(!ro && manager.hasActiveSession());
        addToSessionBtn.setEnabled(!ro && manager.hasActiveSession());
        removeFromSessionBtn.setEnabled(!ro && manager.hasActiveSession());
        addKillBtn.setEnabled(!ro && manager.hasActiveSession());
    }

    private static class RemoveButtonRenderer extends JButton implements TableCellRenderer {
        RemoveButtonRenderer() {
            setOpaque(true);
        }
        @Override
        public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText("X");
            return this;
        }
    }

    private final class MetricsModel extends AbstractTableModel {
        private Session session;
        private List<SessionManager.PlayerMetrics> rows = List.of();

        void setData(Session session, List<SessionManager.PlayerMetrics> rows) {
            this.session = session;
            this.rows = rows;
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return 4;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return "Player";
                case 1:
                    return "Total";
                case 2:
                    return "Split";
                case 3:
                    return "Remove";
                default:
                    return "";
            }
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 3) {
                return JButton.class;
            }
            return Object.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 3;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            var r = rows.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return r.player;
                case 1:
                    return DF.format(r.total);
                case 2:
                    return DF.format(r.split);
                case 3:
                    return "X";
                default:
                    return "";
            }
        }
    }


    private class RemoveButtonEditor extends DefaultCellEditor {
        private final JButton button = new JButton("X");
        private int row = -1;

        RemoveButtonEditor() {
            super(new JCheckBox());
            button.addActionListener(e -> {
                if (row >= 0) {
                    String player = (String) metricsTable.getModel().getValueAt(row, 0);
                    if (manager.removePlayerFromActive(player)) {
                        toast("Player removed.");
                        refreshUi.run();
                    } else {
                        toast("Failed to remove player.");
                    }
                }
                fireEditingStopped();
            });
        }

        @Override
        public java.awt.Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            this.row = row;
            button.setText("X");
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            return "X";
        }
    }


    private void onAddPeep(ActionEvent e) {
        String name = newPeepField.getText().trim();
        if (name.isEmpty()) {
            toast("Enter a name.");
            return;
        }
        if (!manager.addPeep(name)) {
            toast("Player already in list exists.");
            return;
        }
        // Clear and make the newly added name selected in the single dropdown
        newPeepField.setText("");
        manager.saveToConfig();
        refreshUi.run();
        peepDropdown.setSelectedItem(name);
        newPeepField.requestFocusInWindow();
    }

    private void onRemovePeep(ActionEvent e) {
        String selected = (String) peepDropdown.getSelectedItem();
        if (selected == null) {
            toast("Select a peep to remove.");
            return;
        }
        if (!manager.removePeep(selected)) {
            toast("Not found.");
            return;
        }
        manager.saveToConfig();
        refreshUi.run();
    }

    // ... existing code (onStartSession, onStopSession, onAddToSession, onRemoveFromSession, onAddKill, etc.) ...
}