package com.example.pksession.panel;

import com.example.pksession.Formats;
import com.example.pksession.PkSessionConfig;
import com.example.pksession.SessionManager;
import com.example.pksession.model.Metrics;
import com.example.pksession.model.RecentSplitsTable;
import com.example.pksession.model.Session;
import lombok.Getter;
import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import javax.swing.text.DefaultFormatterFactory;
import java.awt.*;

public class PanelView extends PluginPanel {
    private final com.example.pksession.SessionManager manager;
    private final PkSessionConfig config;

    @Getter
    private final JComboBox<String> knownPlayersDropdown = new JComboBox<>();
    @Getter
    private final JTextField newPeepField = new JTextField();
    @Getter
    private final JLabel historyLabel = new JLabel("History: OFF");
    @Getter
    private final JFormattedTextField killAmountField = makeOsrsField();
    @Getter
    private final JTable metricsTable = new JTable(new Metrics());
    @Getter
    private final RecentSplitsTable recentSplitsModel = new RecentSplitsTable();
    @Getter
    private final JTable recentSplitsTable = makeRecentSplitsTable(recentSplitsModel);

    @Getter
    private final JButton btnAddPeep = new JButton("Add peep");
    @Getter
    private final JButton btnRemovePeep = new JButton("Remove");
    @Getter
    private final JButton btnAddKill = new JButton("Add");
    @Getter
    private final JButton btnStart = new JButton("Start");
    @Getter
    private final JButton btnStop = new JButton("Stop");
    @Getter
    private final JButton btnAddToSession = new JButton("Add");
    @Getter
    private final JButton btnRemoveFromSession = new JButton("Remove");

    @Getter
    private final JComboBox<String> currentSessionPlayerDropdown = new JComboBox<>();
    @Getter
    private final JComboBox<String> notInCurrentSessionPlayerDropdown = new JComboBox<>();
    @Getter
    private final DefaultListModel<Session> historyModel = new DefaultListModel<>();
    @Getter
    private final JList<Session> historyList = new JList<>(historyModel);


    private JFormattedTextField makeOsrsField() {
        JFormattedTextField f = new JFormattedTextField(
                new DefaultFormatterFactory(new Formats.OsrsAmountFormatter()));
        f.setColumns(14);
        f.setFocusLostBehavior(JFormattedTextField.COMMIT_OR_REVERT);
        f.setToolTipText("Enter amount like 10k, 1.1m, or 1b (K = thousands)");
        return f;
    }

    private JTable makeRecentSplitsTable(RecentSplitsTable model) {
        JTable t = new JTable(model);
        t.setFillsViewportHeight(true);
        t.setRowHeight(22);
        t.setShowGrid(false);
        t.setFocusable(false);
        t.setEnabled(false); // read-only

        // Right-align amount col
        javax.swing.table.DefaultTableCellRenderer right = new javax.swing.table.DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        t.getColumnModel().getColumn(2).setCellRenderer(right);

        return t;
    }

    public PanelView(SessionManager manager, PkSessionConfig config) {
        this.manager = manager;
        this.config = config;

        JPanel top = new JPanel();
        top.setLayout(new javax.swing.BoxLayout(top, javax.swing.BoxLayout.Y_AXIS));
        top.add(generateSessionPanel());
        top.add(Box.createVerticalStrut(3));
        top.add(generateSessionPlayerManagement());
        top.add(Box.createVerticalStrut(3));
        top.add(generateAddSplit());
        top.add(Box.createVerticalStrut(3));
        top.add((generateMetrics()));
        top.add(Box.createVerticalStrut(3));
        top.add(generateKnownPlayersManagement());


        JPanel center = new JPanel();
        center.setLayout(new javax.swing.BoxLayout(center, javax.swing.BoxLayout.Y_AXIS));

        add(top, BorderLayout.NORTH);
    }

    private JPanel generateAddSplit(){
        JPanel killPanel = new JPanel();
        killPanel.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createTitledBorder("Add split to session:"),
                        BorderFactory.createEmptyBorder(3, 3, 3, 3)
                )
        );

        killPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 3, 3, 3);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        Dimension dV = new Dimension(96, 24);
        Dimension dl = new Dimension(48, 24);

        killAmountField.setColumns(14);
        currentSessionPlayerDropdown.setPreferredSize(dV);
        killAmountField.setPreferredSize(dV);

        // Row 0: Player label + dropdown
        JLabel apLabel = new JLabel("Player:");
        apLabel.setPreferredSize(dl);
        apLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.EAST;
        killPanel.add(apLabel, gbc);

        gbc.gridx = 1; gbc.gridy = 0;
        gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.anchor = GridBagConstraints.WEST;
        killPanel.add(currentSessionPlayerDropdown, gbc);

        // Row 1: Amount label + field
        JLabel amountLabel = new JLabel("Amount:");
        amountLabel.setPreferredSize(dl);
        amountLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        gbc.gridx = 0; gbc.gridy = 1;
        gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.EAST;
        killPanel.add(amountLabel, gbc);

        gbc.gridx = 1; gbc.gridy = 1;
        gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.anchor = GridBagConstraints.WEST;
        killPanel.add(killAmountField, gbc);

        // Row 2: Add button
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.anchor = GridBagConstraints.EAST;
        killPanel.add(btnAddKill, gbc);

        // Row 3: Last 10 splits table (spans 2)
        JPanel recentSplits = new JPanel(new BorderLayout());
        recentSplits.setBorder(BorderFactory.createTitledBorder("Recent splits"));
        JScrollPane scroller = new JScrollPane(recentSplitsTable);
        scroller.setPreferredSize(new Dimension(0, 140)); // height hint
        recentSplits.add(scroller, BorderLayout.CENTER);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        gbc.weightx = 1.0; gbc.fill = GridBagConstraints.BOTH; gbc.anchor = GridBagConstraints.CENTER;
        gbc.weighty = 1.0; // let the table grow
        killPanel.add(recentSplits, gbc);

        // Wire the button (avoid duplicate listeners if called multiple times)
        for (java.awt.event.ActionListener al : btnAddKill.getActionListeners()) {
            if (al.getClass().getName().endsWith("AddKillHandler")) {
                // already wired by us
                return killPanel;
            }
        }

        return killPanel;
    }

    private JPanel generateSessionPlayerManagement() {
        JPanel rosterPanel = new JPanel();
        rosterPanel.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createTitledBorder("Add players to session:"),
                        BorderFactory.createEmptyBorder(3, 3, 3, 3)
                )
        );

        // Layout
        rosterPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 3, 3, 3);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Build the dropdown containing only non-active players
        notInCurrentSessionPlayerDropdown.setPreferredSize(new Dimension(94, 24));
        btnAddToSession.setPreferredSize(new Dimension(64, 24));

        String[] peeps = manager.getNonActivePlayers().toArray(new String[0]);
        notInCurrentSessionPlayerDropdown.setModel(new DefaultComboBoxModel<>(peeps));

        // Row 0: dropdown (expand) + button (fixed)
        gbc.gridx = 1; gbc.gridy = 0;
        gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL;
        rosterPanel.add(notInCurrentSessionPlayerDropdown, gbc);

        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE;
        rosterPanel.add(btnAddToSession, gbc);

        // Row 1: concise help label (centered, spans 2 cols)
        JLabel help = new JLabel("Remove via the 'X' in the table.");
        help.setHorizontalAlignment(SwingConstants.CENTER);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2;
        gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.anchor = GridBagConstraints.CENTER;
        rosterPanel.add(help, gbc);

        return rosterPanel;
    }


    private JPanel generateKnownPlayersManagement() {
        JPanel peepsPanel = new JPanel();
        peepsPanel.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createTitledBorder("Edit known players:"),
                        BorderFactory.createEmptyBorder(3, 3, 3, 3)
                )
        );

        peepsPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 3, 3, 3);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Sizing
        Dimension dl = new Dimension(64, 24);     // label width
        Dimension dField = new Dimension(135, 24);
        Dimension dDrop = new Dimension(128, 24);

        knownPlayersDropdown.setPreferredSize(dDrop);

        newPeepField.setEditable(true);
        newPeepField.setFocusable(true);
        newPeepField.setColumns(14);
        newPeepField.setPreferredSize(dField);
        newPeepField.setToolTipText("Type a name and click 'Add peep'");
        newPeepField.setOpaque(true);

        // Row 0: Name
        JLabel nameLabel = new JLabel("Name:");
        nameLabel.setPreferredSize(dl);
        nameLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.EAST;
        peepsPanel.add(nameLabel, gbc);

        gbc.gridx = 1; gbc.gridy = 0;
        gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.anchor = GridBagConstraints.WEST;
        peepsPanel.add(newPeepField, gbc);

        // Row 1: List
        JLabel listLabel = new JLabel("Known:");
        listLabel.setPreferredSize(dl);
        listLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        gbc.gridx = 0; gbc.gridy = 1;
        gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.EAST;
        peepsPanel.add(listLabel, gbc);

        gbc.gridx = 1; gbc.gridy = 1;
        gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.anchor = GridBagConstraints.WEST;
        peepsPanel.add(knownPlayersDropdown, gbc);

        // Row 2: Add / Remove buttons (span 2, 50/50)
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 6, 0));
        buttonPanel.add(btnAddPeep);     // <-- Add
        buttonPanel.add(btnRemovePeep);  // <-- Remove (added back)

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.anchor = GridBagConstraints.CENTER;
        peepsPanel.add(buttonPanel, gbc);

        // Row 3: Session panel (span 2)
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.anchor = GridBagConstraints.WEST;

        return peepsPanel;
    }


    private JPanel generateSessionPanel() {
        JPanel sessionPanel = new JPanel();
        sessionPanel.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createTitledBorder("Session"),
                        BorderFactory.createEmptyBorder(3, 3, 3, 3)
                )
        );

        sessionPanel.setLayout(new GridBagLayout());
        GridBagConstraints g2 = new GridBagConstraints();
        g2.gridx = 0;
        g2.insets = new Insets(3, 3, 3, 3);
        g2.weightx = 1.0;
        g2.fill = GridBagConstraints.HORIZONTAL;
        g2.anchor = GridBagConstraints.CENTER;

        // Row 0: buttons in a 1x2 GridLayout so each takes 50% width
        JPanel buttonsHalfHalf = new JPanel(new GridLayout(1, 2, 6, 0));
        buttonsHalfHalf.add(btnStart);
        buttonsHalfHalf.add(btnStop);

        g2.gridy = 0;
        sessionPanel.add(buttonsHalfHalf, g2);

        // Row 1: centered label spanning full width
        historyLabel.setHorizontalAlignment(SwingConstants.CENTER);

        g2.gridy = 1;
        sessionPanel.add(historyLabel, g2);

        return sessionPanel;
    }



    private JPanel generateHistory(){
        JPanel historyPanel = new JPanel();
//        historyPanel.setBorder(
//                BorderFactory.createCompoundBorder(
//                        BorderFactory.createTitledBorder("Past Sessions"),
//                        BorderFactory.createEmptyBorder(3, 3, 64, 3)
//                )
//        );
//        historyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
//        historyList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
//            JLabel lbl = new JLabel(sessionLabel(value));
//            if (isSelected) {
//                lbl.setOpaque(true);
//            }
//            return lbl;
//        });
//        JScrollPane histScroll = new JScrollPane(historyList);
//        histScroll.setPreferredSize(new Dimension(320, 160));
//        histScroll.setBorder(
//                BorderFactory.createCompoundBorder(
//                        histScroll.getBorder(),
//                        BorderFactory.createEmptyBorder(3, 3, 64, 3)
//                )
//        );
//        historyPanel.add(histScroll);
//        JButton loadHistoryBtn = new JButton("Load selected");
//        historyPanel.add(loadHistoryBtn);
//        JButton unloadHistoryBtn = new JButton("Unload history");
//        historyPanel.add(unloadHistoryBtn);
//
//        JPanel center = new JPanel();
//        center.setLayout(new javax.swing.BoxLayout(center, javax.swing.BoxLayout.Y_AXIS));
//        center.add(tableScroll);
//        center.add(Box.createVerticalStrut(3));
//        center.add(historyPanel);

        return historyPanel;
    }

    private JScrollPane generateMetrics(){
        // Set up the metricsTable with custom column configuration

        metricsTable.setFillsViewportHeight(true);
        metricsTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        metricsTable.setFillsViewportHeight(true);
        JScrollPane tableScroll = new JScrollPane(metricsTable);
        tableScroll.setPreferredSize(new Dimension(320, 360));

        Session currentSession = manager.getCurrentSession().orElse(null);
        ((Metrics) metricsTable.getModel()).setData(currentSession, manager.computeMetricsFor(currentSession, true));

        int tableWidth = metricsTable.getWidth();
        int equalWidth = (int)(tableWidth * 0.33); // Equal width for first 3 columns (33% each)

        // Set widths for first 3 columns
        for (int i = 0; i < 3; i++) {
            metricsTable.getColumnModel().getColumn(i).setPreferredWidth(equalWidth);
        }

        // Set X column to minimum width
        metricsTable.getColumnModel().getColumn(3).setMaxWidth(40);
        metricsTable.getColumnModel().getColumn(3).setMinWidth(40);
        metricsTable.getColumnModel().getColumn(3).setPreferredWidth(40);

        // Grey-out renderer for columns 0-2 when player is non-active
        javax.swing.table.DefaultTableCellRenderer greyingRenderer = new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(JTable table, Object value,
                                                                    boolean isSelected, boolean hasFocus,
                                                                    int row, int column) {
                java.awt.Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                Metrics model = (Metrics) table.getModel();
                boolean active = model.isRowActive(row);
                // Slightly grey out non-active rows; keep selection colors if selected
                if (!isSelected) {
                    c.setForeground(active ? table.getForeground() : java.awt.Color.GRAY);
                }
                return c;
            }
        };
        metricsTable.getColumnModel().getColumn(0).setCellRenderer(greyingRenderer);
        metricsTable.getColumnModel().getColumn(1).setCellRenderer(greyingRenderer);
        metricsTable.getColumnModel().getColumn(2).setCellRenderer(greyingRenderer);

        // Renderer for column 3: show a button "X" for active, and a greyed label "💤" for non-active
        javax.swing.table.TableCellRenderer actionRenderer = new javax.swing.table.TableCellRenderer() {
            private final JButton btn = new JButton("X");
            private final JLabel sleeping = new JLabel("💤", SwingConstants.CENTER);
            @Override
            public java.awt.Component getTableCellRendererComponent(JTable table, Object value,
                                                                    boolean isSelected, boolean hasFocus,
                                                                    int row, int column) {
                Metrics model = (Metrics) table.getModel();
                boolean active = model.isRowActive(row);
                if (active) {
                    btn.setText("X");
                    // Keep button styling; respect selection background
                    if (isSelected) {
                        btn.setBackground(table.getSelectionBackground());
                        btn.setForeground(table.getSelectionForeground());
                    } else {
                        btn.setBackground(UIManager.getColor("Button.background"));
                        btn.setForeground(UIManager.getColor("Button.foreground"));
                    }
                    return btn;
                } else {
                    sleeping.setText("💤");
                    sleeping.setOpaque(false);
                    if (!isSelected) {
                        sleeping.setForeground(java.awt.Color.GRAY);
                    } else {
                        sleeping.setForeground(table.getSelectionForeground());
                        sleeping.setBackground(table.getSelectionBackground());
                        sleeping.setOpaque(true);
                    }
                    return sleeping;
                }
            }
        };
        metricsTable.getColumnModel().getColumn(3).setCellRenderer(actionRenderer);

        // Keep the remove editor; it will only activate for active rows (see Metrics.isCellEditable)
        metricsTable.getColumnModel().getColumn(3).setCellEditor(new TableRemoveButtonEditor(this,manager,metricsTable));

        return tableScroll;

    }

}
