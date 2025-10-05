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
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.text.DefaultFormatterFactory;
import java.awt.*;
import java.awt.datatransfer.StringSelection;

@Getter
public class PanelView extends PluginPanel {
    // New dynamic panel for active player management buttons (stack one player per row)
    private final JPanel activePlayersButtonsPanel = new JPanel(new GridLayout(0, 1, 0, 2));
    private final com.example.pksession.SessionManager manager;
    private final PkSessionConfig config;

    private final JComboBox<String> knownPlayersDropdown = new JComboBox<>();
    private final JTextField newPeepField = new JTextField();
    private final JLabel historyLabel = new JLabel("History: OFF");
    private final JFormattedTextField killAmountField = makeOsrsField();
    // Separate amount field for the Active Player Management section to avoid component reparenting
    private final JFormattedTextField activeKillAmountField = makeOsrsField();
    private final JTable metricsTable = new JTable(new Metrics());
    private final RecentSplitsTable recentSplitsModel = new RecentSplitsTable();
    private JTable recentSplitsTable;

    // Waitlist UI (table)
    private final com.example.pksession.model.WaitlistTableModel waitlistTableModel = new com.example.pksession.model.WaitlistTableModel();
    private final JTable waitlistTable = new JTable(waitlistTableModel);
    private final JButton btnWaitlistAdd = new JButton("Add");
    private final JButton btnWaitlistDelete = new JButton("Del");

    private final JButton btnAddPeep = new JButton("Add peep");
    // Labels for dynamic text
    private final JLabel knownListLabel = new JLabel("Known:");
    private final JLabel altsLabel = new JLabel("Known alts:");
    // Shows when the selected player is an alt of someone
    private final JLabel altOfLabel = new JLabel("");
    // Alt/Main UI (new): list of alts for selected player, and controls to add an alt
    private final JList<String> altsList = new JList<>(new DefaultListModel<>());
    private final JComboBox<String> addAltDropdown = new JComboBox<>();
    private final JButton btnAddAlt = new JButton("Add alt");
    private final JButton btnRemoveAlt = new JButton("Remove alt");
    private final JButton btnRemovePlayer = new JButton("Remove player");
    private final JButton btnAddKill = new JButton("Add");
    private final JButton btnStart = new JButton("Start");
    private final JButton btnStop = new JButton("Stop");
    private final JButton btnAddToSession = new JButton("Add");
    private final JButton btnRemoveFromSession = new JButton("Remove");

    private final JComboBox<String> currentSessionPlayerDropdown = new JComboBox<>();
    private final JComboBox<String> notInCurrentSessionPlayerDropdown = new JComboBox<>();
    private final DefaultListModel<Session> historyModel = new DefaultListModel<>();
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
        t.setFocusable(true);
        t.setEnabled(true); // editable via model

        // Right-align amount col
        javax.swing.table.DefaultTableCellRenderer right = new javax.swing.table.DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        t.getColumnModel().getColumn(2).setCellRenderer(right);

        // Player editor: combo of current session players (fallback to known mains)
        JComboBox<String> playerEditorCombo = new JComboBox<>();
        {
            java.util.Set<String> choices;
            Session curr = manager.getCurrentSession().orElse(null);
            if (curr != null && !curr.getPlayers().isEmpty())
                choices = new java.util.LinkedHashSet<>(curr.getPlayers());
            else choices = new java.util.LinkedHashSet<>(manager.getKnownMains());
            playerEditorCombo.setModel(new DefaultComboBoxModel<>(choices.toArray(new String[0])));
        }
        t.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(playerEditorCombo));

        // Amount editor: formatted text field accepting K/M/B
        JFormattedTextField amtField = new JFormattedTextField(new DefaultFormatterFactory(new Formats.OsrsAmountFormatter()));
        amtField.setBorder(null);
        DefaultCellEditor amtEditor = new DefaultCellEditor(amtField);
        t.getColumnModel().getColumn(2).setCellEditor(amtEditor);

        return t;
    }

    public PanelView(SessionManager manager, PkSessionConfig config) {
        this.manager = manager;
        this.config = config;
        // React to edits in recent splits to update metrics
        recentSplitsModel.setListener(this::refreshMetrics);

        recentSplitsTable = makeRecentSplitsTable(recentSplitsModel);

        JPanel top = new JPanel();
        top.setLayout(new javax.swing.BoxLayout(top, javax.swing.BoxLayout.Y_AXIS));

        java.util.Map<String, java.util.function.Supplier<java.awt.Component>> sections = new java.util.LinkedHashMap<>();
        // New active player management section at the very top by default (toggle via settings)
        if (config.useActivePlayerManagement()) {
            sections.put("activeplayermgmt", this::generateActivePlayerManagement);
        }
        sections.put("session", this::generateSessionPanel);
        sections.put("sessionplayers", this::generateSessionPlayerManagement);
        sections.put("addsplit", this::generateAddSplit);
        sections.put("recentsplits", this::generateRecentSplitsPanel);
        sections.put("detectedvalues", this::generateWaitlistPanelCollapsible);
        sections.put("settlement", this::generateMetrics);
        sections.put("knownplayers", this::generateKnownPlayersManagement);

        java.util.Set<String> added = new java.util.LinkedHashSet<>();
        String orderCsv = config.sectionOrderCsv();
        if (orderCsv != null && !orderCsv.isBlank()) {
            for (String key : orderCsv.split(",")) {
                String k = key.trim().toLowerCase();
                java.util.function.Supplier<java.awt.Component> sup = sections.get(k);
                if (sup != null) {
                    top.add(sup.get());
                    top.add(Box.createVerticalStrut(3));
                    added.add(k);
                }
            }
        }
        for (var e : sections.entrySet()) {
            if (!added.contains(e.getKey())) {
                top.add(e.getValue().get());
                top.add(Box.createVerticalStrut(3));
            }
        }

        add(top, BorderLayout.NORTH);
    }

    private JPanel generateAddSplit() {
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

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        killPanel.add(apLabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        killPanel.add(currentSessionPlayerDropdown, gbc);

        // Row 1: Amount label + field
        JLabel amountLabel = new JLabel("Amount:");
        amountLabel.setPreferredSize(dl);
        amountLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        killPanel.add(amountLabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        killPanel.add(killAmountField, gbc);

        // Row 2: Add button
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.EAST;
        killPanel.add(btnAddKill, gbc);

        // (Recent splits moved to its own section)

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
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        rosterPanel.add(notInCurrentSessionPlayerDropdown, gbc);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        rosterPanel.add(btnAddToSession, gbc);

        // Row 1: concise help label (centered, spans 2 cols)
        JLabel help = new JLabel("Remove via the 'X' in the table.");
        help.setHorizontalAlignment(SwingConstants.CENTER);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;
        rosterPanel.add(help, gbc);

        return rosterPanel;
    }


    private JPanel generateKnownPlayersManagement() {
        JPanel peepsPanel = new JPanel();

        peepsPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 3, 3, 3);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Your sizing pattern
        Dimension dV = new Dimension(96, 24);  // input/dropdown width
        Dimension dl = new Dimension(48, 24);  // label width

        // Prepare components
        JLabel title = new JLabel("Edit the known players list:");
        title.setFont(title.getFont().deriveFont(Font.BOLD));

        newPeepField.setColumns(14);
        newPeepField.setPreferredSize(dV);
        knownPlayersDropdown.setPreferredSize(dV);
        addAltDropdown.setPreferredSize(dV);

        int row = 0;

        // Row 0: Title (100%)
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        peepsPanel.add(title, gbc);
        row++;

        // Row 1: Name lbl + field
        JLabel nameLabel = new JLabel("Name:");
        nameLabel.setPreferredSize(dl);
        nameLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        gbc.gridwidth = 1;

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        peepsPanel.add(nameLabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        peepsPanel.add(newPeepField, gbc);
        row++;

        // Row 2: Add player (100%)
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;
        peepsPanel.add(btnAddPeep, gbc);
        row++;

        // Row 3: <hr> (100%)
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;
        peepsPanel.add(new JSeparator(SwingConstants.HORIZONTAL), gbc);
        row++;

        // Row 4: "Alter player info" label ON ITS OWN ROW (100%)
        JLabel alterLbl = new JLabel("Alter player info:");
        alterLbl.setHorizontalAlignment(SwingConstants.LEFT);
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        peepsPanel.add(alterLbl, gbc);
        row++;

        // Row 5: Player select dropdown (single item, 100%)
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        peepsPanel.add(knownPlayersDropdown, gbc);
        row++;

        // Row 6: Remove player (100%) — directly under the dropdown
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;
        peepsPanel.add(btnRemovePlayer, gbc);
        row++;

        // Row 7: "Player known alts" label (own row, 100%)
        altsLabel.setHorizontalAlignment(SwingConstants.LEFT);
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        peepsPanel.add(altsLabel, gbc);
        row++;

        // Row 8: The list (100%, grows vertically)
        JScrollPane altsScroll = new JScrollPane(altsList);
        altsScroll.setPreferredSize(new Dimension(240, 140));
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.CENTER;
        peepsPanel.add(altsScroll, gbc);
        row++;

        // Row 9: Add alt lbl + dropdown
        JLabel addAltLbl = new JLabel("Add alt:");
        addAltLbl.setPreferredSize(dl);
        addAltLbl.setHorizontalAlignment(SwingConstants.RIGHT);

        gbc.gridwidth = 1;
        gbc.weighty = 0;

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        peepsPanel.add(addAltLbl, gbc);

        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        peepsPanel.add(addAltDropdown, gbc);
        row++;

        // Row 10: Add/Remove alt buttons on the same row (full-width container)
        JPanel altButtonsRow = new JPanel(new GridLayout(1, 2, 6, 0));
        altButtonsRow.add(btnAddAlt);
        altButtonsRow.add(btnRemoveAlt);

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;
        peepsPanel.add(altButtonsRow, gbc);
        row++;

        return new CollapsiblePanel("Known player info", peepsPanel);
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


    private JPanel generateHistory() {
        // TODO
        return null;
    }

    private JPanel generateWaitlistPanel() {
        JPanel p = new JPanel();
        p.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        waitlistTable.setFillsViewportHeight(true);
        waitlistTable.setRowHeight(22);
        waitlistTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane sc = new JScrollPane(waitlistTable);
        sc.setPreferredSize(new Dimension(0, 120));
        p.add(sc, gbc);

        JPanel btns = new JPanel(new GridLayout(1, 2, 6, 0));
        btns.add(btnWaitlistAdd);
        btns.add(btnWaitlistDelete);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(3, 0, 0, 0);
        p.add(btns, gbc);

        // Always keep first row selected
        waitlistTableModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if (waitlistTable.getRowCount() > 0) {
                    waitlistTable.getSelectionModel().setSelectionInterval(0, 0);
                } else {
                    waitlistTable.clearSelection();
                }
            }
        });
        if (waitlistTable.getRowCount() > 0) {
            waitlistTable.getSelectionModel().setSelectionInterval(0, 0);
        }

        return p;
    }

    private JComponent generateWaitlistPanelCollapsible() {
        JPanel content = new JPanel(new BorderLayout());
        content.add(generateWaitlistPanel(), BorderLayout.CENTER);

        return new CollapsiblePanel("Detected values", content);
    }


    private JComponent generateRecentSplitsPanel() {
        JScrollPane scroller = new JScrollPane(recentSplitsTable);
        scroller.setPreferredSize(new Dimension(0, 140));
        return new CollapsiblePanel("Recent splits", scroller);
    }

    private JComponent generateMetrics() {
        // Wrapper panel with title and description
        JPanel wrapper = new JPanel(new BorderLayout(0, 6));
        JLabel title = new JLabel("Settlement");
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        JLabel desc = new JLabel("-100k means you owe that amount to the player; 100k means the player owes you that money.");

        JPanel header = new JPanel(new BorderLayout());
        header.add(title, BorderLayout.NORTH);
        header.add(desc, BorderLayout.CENTER);

        JButton copyBtn = new JButton("Copy JSON");
        copyBtn.addActionListener(e -> copyMetricsJsonToClipboard());
        header.add(copyBtn, BorderLayout.EAST);
        wrapper.add(header, BorderLayout.NORTH);

        // Set up the metricsTable with custom column configuration
        metricsTable.setFillsViewportHeight(true);
        metricsTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        JScrollPane tableScroll = new JScrollPane(metricsTable);
        tableScroll.setPreferredSize(new Dimension(320, 360));

        refreshMetrics();

        int tableWidth = Math.max(metricsTable.getWidth(), 1);
        int equalWidth = (int) (tableWidth * 0.33); // Equal width for first 3 columns (33% each)

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
        metricsTable.getColumnModel().getColumn(3).setCellEditor(new TableRemoveButtonEditor(this, manager, metricsTable));

        wrapper.add(tableScroll, BorderLayout.CENTER);
        return new CollapsiblePanel("Settlement information", wrapper);
    }

    private void refreshMetrics() {
        Session currentSession = manager.getCurrentSession().orElse(null);
        ((Metrics) metricsTable.getModel()).setData(manager.computeMetricsFor(currentSession, true));
    }

    private void copyMetricsJsonToClipboard() {
        Session currentSession = manager.getCurrentSession().orElse(null);
        java.util.List<com.example.pksession.SessionManager.PlayerMetrics> data = manager.computeMetricsFor(currentSession, true);
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
        StringSelection selection = new StringSelection(sb.toString());
        java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
    }

    // Build a top-of-panel section with per-active-player buttons
    private JPanel generateActivePlayerManagement() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 3, 3, 3);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Row 0: Amount label + shared amount field (use a dedicated field for this section)
        JLabel amountLabel = new JLabel("Amount:");
        amountLabel.setPreferredSize(new Dimension(48, 24));

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(amountLabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(activeKillAmountField, gbc);

        // Row 1: Scrollable area with one row per active player (buttons)
        JScrollPane scroller = new JScrollPane(activePlayersButtonsPanel);
        scroller.setBorder(null);
        scroller.setPreferredSize(new Dimension(0, 360));
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(scroller, gbc);

        // Initial population
        refreshActivePlayerButtons();
        return new CollapsiblePanel("Active player management", panel);
    }

    // Rebuild the list of per-player buttons from current session
    public void refreshActivePlayerButtons() {
        activePlayersButtonsPanel.removeAll();
        com.example.pksession.model.Session current = manager.getCurrentSession().orElse(null);
        if (current != null && current.isActive()) {
            for (String p : new java.util.ArrayList<>(current.getPlayers())) {
                JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
                JLabel name = new JLabel(p);
                JButton addBtn = new JButton("➕");
                addBtn.setToolTipText("Add split amount to " + p);
                JButton rmBtn = new JButton("🗑");
                rmBtn.setToolTipText("Remove " + p + " from session");

                addBtn.addActionListener(e -> {
                    long amt;
                    Object val = activeKillAmountField.getValue();
                    try {
                        amt = val == null ? Long.parseLong(activeKillAmountField.getText()) : ((Number) val).longValue();
                    } catch (Exception ex) {
                        javax.swing.JOptionPane.showMessageDialog(null, "Invalid amount.", "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    if (manager.addKill(p, amt)) {
                        activeKillAmountField.setText("");
                        com.example.pksession.Utils.requestUiRefresh().run();
                    } else {
                        javax.swing.JOptionPane.showMessageDialog(null, "Failed to add split. Is player in session?", "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
                    }
                });
                rmBtn.addActionListener(e -> {
                    if (manager.removePlayerFromSession(p)) {
                        com.example.pksession.Utils.requestUiRefresh().run();
                    } else {
                        javax.swing.JOptionPane.showMessageDialog(null, "Failed to remove player from session.", "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
                    }
                });

                row.add(addBtn);
                row.add(rmBtn);
                row.add(name);
                // Insert at top so newest rows are always on top
                activePlayersButtonsPanel.add(row, 0);
            }
        } else {
            // Optionally show hint when no active session
            JLabel hint = new JLabel("Start a session to manage players.");
            hint.setForeground(Color.GRAY);
            activePlayersButtonsPanel.add(hint);
        }
        activePlayersButtonsPanel.revalidate();
        activePlayersButtonsPanel.repaint();
    }

}
