package com.splitmanager.views;

import com.splitmanager.ManagerKnownPlayers;
import com.splitmanager.ManagerSession;
import com.splitmanager.PluginConfig;
import com.splitmanager.controllers.PanelActions;
import com.splitmanager.controllers.PanelController;
import com.splitmanager.models.Metrics;
import com.splitmanager.models.PlayerMetrics;
import com.splitmanager.models.RecentSplitsTable;
import com.splitmanager.models.Session;
import com.splitmanager.models.Transfer;
import com.splitmanager.models.WaitlistTable;
import com.splitmanager.utils.Formats;
import static com.splitmanager.utils.Formats.OsrsAmountFormatter.toSuffixString;
import com.splitmanager.utils.MarkdownFormatter;
import com.splitmanager.utils.PaymentProcessor;
import static com.splitmanager.utils.Utils.toast;
import com.splitmanager.views.components.DropdownRip;
import com.splitmanager.views.components.table.RemoveButtonEditor;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.TableModelListener;
import javax.swing.text.DefaultFormatterFactory;
import lombok.Getter;
import net.runelite.client.ui.PluginPanel;

@Getter
/**
 * Swing-based view for the Auto Split Manager panel. Renders sections and forwards
 * user interactions to PanelActions.
 */
public class PanelView extends PluginPanel
{
	private final JPanel activePlayersButtonsPanel = new JPanel(new GridLayout(0, 1, 0, 2));
	private final ManagerSession sessionManager;
	private final PluginConfig config;
	private final ManagerKnownPlayers playerManager;
	private final JComboBox<String> knownPlayersDropdown = new JComboBox<>();
	private final JTextField newPlayerField = new JTextField();
	private final JLabel historyLabel = new JLabel("History: OFF");
	private final JFormattedTextField killAmountField = makeOsrsField();
	private final JFormattedTextField activeKillAmountField = makeOsrsField();
	private final JTable metricsTable = new JTable(new Metrics());
	private final RecentSplitsTable recentSplitsModel = new RecentSplitsTable();
	private final WaitlistTable waitlistTableModel = new WaitlistTable();
	private final JTable waitlistTable = new JTable(waitlistTableModel);
	private final JButton btnWaitlistAdd = new JButton("Add");
	private final JButton btnWaitlistDelete = new JButton("Del");
	private final JButton btnAddPlayer = new JButton("Add Player");
	private final JLabel knownListLabel = new JLabel("Known:");
	private final JLabel altsLabel = new JLabel("Known alts:");
	private final JLabel altOfLabel = new JLabel("");
	private final JList<String> altsList = new JList<>(new DefaultListModel<>());
	private final JComboBox<String> addAltDropdown = new JComboBox<>();
	private final JButton btnAddAlt = new JButton("Add alt");
	private final JButton btnRemoveAlt = new JButton("Remove alt");
	private final JButton btnRemovePlayer = new JButton("Remove");
	private final JButton btnAddKill = new JButton("Add");
	private final JButton btnStart = new JButton("Start");
	private final JButton btnStop = new JButton("Stop");
	private final JButton btnAddToSession = new JButton("Add");
	private final JButton btnRemoveFromSession = new JButton("Remove");
	private final JComboBox<String> currentSessionPlayerDropdown = new JComboBox<>();
	private final JComboBox<String> notInCurrentSessionPlayerDropdown = new JComboBox<>();
	private final DefaultListModel<Session> historyModel = new DefaultListModel<>();
	private final JList<Session> historyList = new JList<>(historyModel);
	private final Dimension dl = new Dimension(48, 24);
	private final Dimension dm = new Dimension(64, 24);
	private final Dimension bm = new Dimension(59, 24);
	private final Dimension dv = new Dimension(96, 24);
	private final Dimension d = new Dimension(128, 24);
	private final Insets inset = new Insets(3, 3, 3, 3);
	private final Dimension lm = new Dimension(0, 140);
	private final Dimension ll = new Dimension(0, 280);
	private PanelActions actions;
	private final JTable recentSplitsTable;

	public PanelView(ManagerSession sessionManager, PluginConfig config, ManagerKnownPlayers playerManager)
	{
		this.sessionManager = sessionManager;
		this.config = config;
		this.playerManager = playerManager;

		recentSplitsModel.setListener(this::refreshMetrics);
		recentSplitsTable = makeRecentSplitsTable(recentSplitsModel);

		JPanel top = new JPanel();
		top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

		top.add(generateSessionPanel());
		top.add(Box.createVerticalStrut(3));
		top.add(generateSessionPlayerManagement());
		top.add(Box.createVerticalStrut(3));
		top.add(generateAddSplit());
		top.add(Box.createVerticalStrut(3));
		top.add(generateRecentSplitsPanel());
		top.add(Box.createVerticalStrut(3));
		top.add(generateWaitlistPanelCollapsible());
		top.add(Box.createVerticalStrut(3));
		top.add(generateMetrics());
		top.add(Box.createVerticalStrut(3));
		top.add(generateKnownPlayersManagement());
		top.add(Box.createVerticalStrut(3));

		add(top, BorderLayout.NORTH);
	}

	private static String shortenName(String name, int maxLen)
	{
		if (name == null)
		{
			return "";
		}
		String n = name.trim();
		return n.length() <= maxLen ? n : n.substring(0, maxLen);
	}

	public void bindActions(PanelActions actions)
	{
		this.actions = actions;

		btnStart.addActionListener(e -> actions.startSession());
		btnStop.addActionListener(e -> actions.stopSession());

		btnAddToSession.addActionListener(e ->
			actions.addPlayerToSession((String) notInCurrentSessionPlayerDropdown.getSelectedItem()));

		btnAddPlayer.addActionListener(e ->
			actions.addKnownPlayer(newPlayerField.getText()));
		btnRemovePlayer.addActionListener(e ->
			actions.removeKnownPlayer((String) knownPlayersDropdown.getSelectedItem()));
		knownPlayersDropdown.addItemListener(e -> {
			if (e.getStateChange() == java.awt.event.ItemEvent.SELECTED)
			{
				actions.onKnownPlayerSelectionChanged((String) e.getItem());
			}
		});

		btnAddAlt.addActionListener(e ->
			actions.addAltToMain((String) knownPlayersDropdown.getSelectedItem(),
				(String) addAltDropdown.getSelectedItem()));
		btnRemoveAlt.addActionListener(e ->
			actions.removeSelectedAlt((String) knownPlayersDropdown.getSelectedItem(),
				altsList.getSelectedValue()));

		btnAddKill.addActionListener(e -> {
			String player = (String) currentSessionPlayerDropdown.getSelectedItem();
			long amt;
			Object val = killAmountField.getValue();
			try
			{
				amt = val == null ? Long.parseLong(killAmountField.getText()) : ((Number) val).longValue();
			}
			catch (Exception ex)
			{
				toast(this, "Invalid amount.");
				return;
			}
			actions.addKill(player, amt);
		});

		btnWaitlistAdd.addActionListener(e -> actions.applySelectedPendingValue(waitlistTable.getSelectedRow()));
		btnWaitlistDelete.addActionListener(e -> actions.deleteSelectedPendingValue(waitlistTable.getSelectedRow()));
	}

	private JFormattedTextField makeOsrsField()
	{
		JFormattedTextField f = new JFormattedTextField(
			new DefaultFormatterFactory(new Formats.OsrsAmountFormatter()));
		f.setColumns(14);
		f.setFocusLostBehavior(JFormattedTextField.COMMIT_OR_REVERT);
		f.setToolTipText("Enter amount like 10k, 1.1m, or 1b (K = thousands)");
		return f;
	}

	private JTable makeRecentSplitsTable(RecentSplitsTable model)
	{
		JTable t = new JTable(model);
		t.setFillsViewportHeight(true);
		t.setRowHeight(22);
		t.setShowGrid(false);
		t.setFocusable(true);
		t.setEnabled(true);

		javax.swing.table.DefaultTableCellRenderer right = new javax.swing.table.DefaultTableCellRenderer();
		right.setHorizontalAlignment(SwingConstants.RIGHT);
		t.getColumnModel().getColumn(2).setCellRenderer(right);

		JComboBox<String> playerEditorCombo = new JComboBox<>();
		{
			java.util.Set<String> choices;
			Session curr = sessionManager.getCurrentSession().orElse(null);
			if (curr != null && !curr.getPlayers().isEmpty())
			{
				choices = new java.util.LinkedHashSet<>(curr.getPlayers());
			}
			else
			{
				choices = new java.util.LinkedHashSet<>(playerManager.getKnownMains());
			}
			playerEditorCombo.setModel(new DefaultComboBoxModel<>(choices.toArray(new String[0])));
		}
		t.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(playerEditorCombo));

		JFormattedTextField amtField = new JFormattedTextField(new DefaultFormatterFactory(new Formats.OsrsAmountFormatter()));
		amtField.setBorder(null);
		DefaultCellEditor amtEditor = new DefaultCellEditor(amtField);
		t.getColumnModel().getColumn(2).setCellEditor(amtEditor);

		return t;
	}

	private JPanel generateAddSplit()
	{
		JPanel killPanel = new JPanel();
		killPanel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createTitledBorder("Add split to session:"),
			BorderFactory.createEmptyBorder(3, 3, 3, 3)));
		killPanel.setLayout(new GridBagLayout());

		GridBagLayout gbl1 = (GridBagLayout) killPanel.getLayout();
		gbl1.columnWidths = new int[]{dm.width, 0};     // col0 fixed, col1 auto
		gbl1.columnWeights = new double[]{0.0, 1.0};    // col0 no grow, col1 grows

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = inset;

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0;
		gbc.anchor = GridBagConstraints.EAST;

		JLabel apLabel = new JLabel("Player:");
		apLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		killPanel.add(apLabel, gbc);

		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		killPanel.add(currentSessionPlayerDropdown, gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.weightx = 0;

		JLabel amountLabel = new JLabel("Amount:");
		amountLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		killPanel.add(amountLabel, gbc);

		gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		killPanel.add(killAmountField, gbc);

		gbc.gridx = 1;
		gbc.gridy = 2;
		gbc.weightx = 1.0;
		gbc.anchor = GridBagConstraints.EAST;
		gbc.fill = GridBagConstraints.NONE;
		btnAddKill.setPreferredSize(dv);
		btnAddKill.setMinimumSize(dv);
		killPanel.add(btnAddKill, gbc);

		return killPanel;
	}

	private JPanel generateSessionPlayerManagement()
	{
		JPanel rosterPanel = new JPanel();
		rosterPanel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createTitledBorder("Add players to session:"),
			BorderFactory.createEmptyBorder(3, 3, 3, 3)));
		rosterPanel.setLayout(new GridBagLayout());

		GridBagLayout gbl1 = (GridBagLayout) rosterPanel.getLayout();
		gbl1.columnWidths = new int[]{dm.width, 0};     // col0 fixed, col1 auto
		gbl1.columnWeights = new double[]{0.0, 1.0};

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = inset;

		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;

		String[] Players = sessionManager.getNonActivePlayers().toArray(new String[0]);
		notInCurrentSessionPlayerDropdown.setModel(new DefaultComboBoxModel<>(Players));
		rosterPanel.add(notInCurrentSessionPlayerDropdown, gbc);

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0;


		JLabel apLabel = new JLabel("Player:");
		apLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		rosterPanel.add(apLabel, gbc);

/*        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.CENTER;

        JLabel help = new JLabel("Remove via the 'X' in the table.");
        help.setHorizontalAlignment(SwingConstants.CENTER);
        rosterPanel.add(help, gbc);*/


		gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.weightx = 1.0;
		gbc.anchor = GridBagConstraints.EAST;
		gbc.fill = GridBagConstraints.NONE;
		btnAddToSession.setPreferredSize(dv);
		rosterPanel.add(btnAddToSession, gbc);

		return rosterPanel;
	}

	private JPanel generateKnownPlayersManagement()
	{
		JPanel PlayersPanel = new JPanel();
		PlayersPanel.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = inset;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;

		JLabel title = new JLabel("Edit the known players list:");
		title.setFont(title.getFont().deriveFont(Font.BOLD));

		newPlayerField.setColumns(14);
		newPlayerField.setPreferredSize(dv);
		knownPlayersDropdown.setPreferredSize(dv);
		addAltDropdown.setPreferredSize(dv);

		int row = 0;

		gbc.gridx = 0;
		gbc.gridy = row;
		gbc.gridwidth = 2;
		gbc.weightx = 1.0;
		PlayersPanel.add(title, gbc);
		row++;

		JLabel nameLabel = new JLabel("Name:");
		nameLabel.setPreferredSize(dl);
		nameLabel.setHorizontalAlignment(SwingConstants.RIGHT);

		gbc.gridwidth = 1;
		gbc.gridx = 0;
		gbc.gridy = row;
		gbc.weightx = 0;
		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.EAST;
		PlayersPanel.add(nameLabel, gbc);

		gbc.gridx = 1;
		gbc.gridy = row;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.WEST;
		PlayersPanel.add(newPlayerField, gbc);
		row++;

		gbc.gridx = 0;
		gbc.gridy = row;
		gbc.gridwidth = 2;
		gbc.weightx = 1.0;
		gbc.anchor = GridBagConstraints.EAST;
		gbc.fill = GridBagConstraints.NONE;
		btnAddPlayer.setPreferredSize(dv);
		btnAddPlayer.setMinimumSize(dv);
		PlayersPanel.add(btnAddPlayer, gbc);
		row++;

		gbc.gridx = 0;
		gbc.gridy = row;
		gbc.gridwidth = 2;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.CENTER;
		PlayersPanel.add(new JSeparator(SwingConstants.HORIZONTAL), gbc);
		row++;

		JLabel alterLbl = new JLabel("Alter player info:");
		alterLbl.setHorizontalAlignment(SwingConstants.LEFT);
		gbc.gridx = 0;
		gbc.gridy = row;
		gbc.gridwidth = 2;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.WEST;
		PlayersPanel.add(alterLbl, gbc);
		row++;

		gbc.gridx = 0;
		gbc.gridy = row;
		gbc.gridwidth = 2;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.WEST;
		PlayersPanel.add(knownPlayersDropdown, gbc);
		row++;

		gbc.gridx = 0;
		gbc.gridy = row;
		gbc.gridwidth = 2;
		gbc.weightx = 1.0;
		gbc.anchor = GridBagConstraints.EAST;
		gbc.fill = GridBagConstraints.NONE;
		btnRemovePlayer.setPreferredSize(dv);
		btnRemovePlayer.setMinimumSize(dv);
		PlayersPanel.add(btnRemovePlayer, gbc);
		row++;

		gbc.gridx = 0;
		gbc.gridy = row;
		gbc.gridwidth = 2;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.CENTER;
		PlayersPanel.add(new JSeparator(SwingConstants.HORIZONTAL), gbc);
		row++;

		altsLabel.setHorizontalAlignment(SwingConstants.LEFT);
		gbc.gridx = 0;
		gbc.gridy = row;
		gbc.gridwidth = 2;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.WEST;
		PlayersPanel.add(altsLabel, gbc);
		row++;

		JScrollPane altsScroll = new JScrollPane(altsList);
		altsScroll.setPreferredSize(lm);
		gbc.gridx = 0;
		gbc.gridy = row;
		gbc.gridwidth = 2;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.anchor = GridBagConstraints.CENTER;
		PlayersPanel.add(altsScroll, gbc);
		row++;

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
		PlayersPanel.add(addAltLbl, gbc);

		gbc.gridx = 1;
		gbc.gridy = row;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.WEST;
		PlayersPanel.add(addAltDropdown, gbc);
		row++;

		JPanel altButtonsRow = new JPanel(new GridLayout(1, 2, 6, 0));
		altButtonsRow.add(btnAddAlt);
		altButtonsRow.add(btnRemoveAlt);

		gbc.gridx = 0;
		gbc.gridy = row;
		gbc.gridwidth = 2;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.CENTER;
		PlayersPanel.add(altButtonsRow, gbc);

		return new DropdownRip("Known player info", PlayersPanel, false);
	}

	private JPanel generateSessionPanel()
	{
		JPanel sessionPanel = new JPanel();
		sessionPanel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createTitledBorder("Session"),
			BorderFactory.createEmptyBorder(3, 3, 3, 3)));
		sessionPanel.setLayout(new GridBagLayout());
		GridBagConstraints g2 = new GridBagConstraints();
		g2.gridx = 0;
		g2.insets = new Insets(3, 3, 3, 3);
		g2.weightx = 1.0;
		g2.fill = GridBagConstraints.HORIZONTAL;
		g2.anchor = GridBagConstraints.CENTER;

		JPanel buttonsHalfHalf = new JPanel(new GridLayout(1, 2, 6, 0));
		buttonsHalfHalf.add(btnStart);
		buttonsHalfHalf.add(btnStop);

		g2.gridy = 0;
		sessionPanel.add(buttonsHalfHalf, g2);

		historyLabel.setHorizontalAlignment(SwingConstants.CENTER);
		g2.gridy = 1;
		sessionPanel.add(historyLabel, g2);

		return sessionPanel;
	}

	private JPanel generateWaitlistPanel()
	{
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
		sc.setPreferredSize(lm);
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

		waitlistTableModel.addTableModelListener(e -> {
			if (waitlistTable.getRowCount() > 0)
			{
				waitlistTable.getSelectionModel().setSelectionInterval(0, 0);
			}
			else
			{
				waitlistTable.clearSelection();
			}
		});
		if (waitlistTable.getRowCount() > 0)
		{
			waitlistTable.getSelectionModel().setSelectionInterval(0, 0);
		}
		return p;
	}

	private JComponent generateWaitlistPanelCollapsible()
	{
		JPanel content = new JPanel(new BorderLayout());
		content.add(generateWaitlistPanel(), BorderLayout.CENTER);
		return new DropdownRip("Detected values", content, false);
	}

	private JComponent generateRecentSplitsPanel()
	{
		JScrollPane scroller = new JScrollPane(recentSplitsTable);
		scroller.setPreferredSize(new Dimension(0, 140));
		return new DropdownRip("Recent splits", scroller, false);
	}

	private JComponent generateMetrics()
	{
		JPanel wrapper = new JPanel(new BorderLayout(0, 6));
		JLabel title = new JLabel("Settlement");
		title.setFont(title.getFont().deriveFont(Font.BOLD));

		boolean direct = config.directPayments();
		String explanation = direct
			? "Direct payments mode: negatives pay positives directly. We'll suggest who pays whom below."
			: (config.flipSettlementSign()
			? "Middleman mode (flipped): positive Split means you pay the bank; negative means the bank pays you."
			: "Middleman mode: negative Split means you pay the bank; positive means the bank pays you.");
		JLabel desc = new JLabel(explanation);

		JPanel header = new JPanel(new BorderLayout());
		header.add(title, BorderLayout.NORTH);
		header.add(desc, BorderLayout.CENTER);

		JButton copyBtn = new JButton("Copy JSON");
		copyBtn.addActionListener(e -> copyMetricsJsonToClipboard());
		JButton copyMdBtn = new JButton("Copy MD");
		copyMdBtn.addActionListener(e -> copyMetricsMarkdownToClipboard());

		wrapper.add(header, BorderLayout.NORTH);

		metricsTable.setFillsViewportHeight(true);
		metricsTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

		((Metrics) metricsTable.getModel()).setHideTotalColumn(true);
		refreshMetrics();

		int colCount = metricsTable.getColumnModel().getColumnCount();
		if (colCount > 0)
		{
			int actionColViewIndex = colCount - 1;
			int nonActionCols = Math.max(1, colCount - 1);
			int tableWidth = Math.max(metricsTable.getWidth(), 1);
			int equalWidth = (int) (tableWidth * (1.0 / nonActionCols));
			for (int i = 0; i < colCount; i++)
			{
				if (i == actionColViewIndex)
				{
					continue;
				}
				metricsTable.getColumnModel().getColumn(i).setPreferredWidth(equalWidth);
			}
			metricsTable.getColumnModel().getColumn(actionColViewIndex).setMaxWidth(40);
			metricsTable.getColumnModel().getColumn(actionColViewIndex).setMinWidth(40);
			metricsTable.getColumnModel().getColumn(actionColViewIndex).setPreferredWidth(40);
		}

		javax.swing.table.DefaultTableCellRenderer greyingRenderer = new javax.swing.table.DefaultTableCellRenderer()
		{
			@Override
			public java.awt.Component getTableCellRendererComponent(JTable table, Object value,
																	boolean isSelected, boolean hasFocus,
																	int row, int column)
			{
				java.awt.Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				Metrics model = (Metrics) table.getModel();
				boolean active = model.isRowActive(row);
				if (!isSelected)
				{
					c.setForeground(active ? table.getForeground() : java.awt.Color.GRAY);
				}
				return c;
			}
		};
		try
		{
			int playerIdx = metricsTable.getColumnModel().getColumnIndex("Player");
			metricsTable.getColumnModel().getColumn(playerIdx).setCellRenderer(greyingRenderer);
		}
		catch (IllegalArgumentException ignored)
		{
		}
		try
		{
			int totalIdx = metricsTable.getColumnModel().getColumnIndex("Total");
			metricsTable.getColumnModel().getColumn(totalIdx).setCellRenderer(greyingRenderer);
		}
		catch (IllegalArgumentException ignored)
		{
		}

		javax.swing.table.DefaultTableCellRenderer splitRenderer = new javax.swing.table.DefaultTableCellRenderer()
		{
			@Override
			public java.awt.Component getTableCellRendererComponent(JTable table, Object value,
																	boolean isSelected, boolean hasFocus,
																	int row, int column)
			{
				Metrics model = (Metrics) table.getModel();
				boolean active = model.isRowActive(row);
				long raw = model.getRawSplitAt(row);
				long disp = raw;
				if (!config.directPayments() && config.flipSettlementSign())
				{
					disp = -raw;
				}
				java.text.DecimalFormat df = Formats.getDecimalFormat();
				java.awt.Component c = super.getTableCellRendererComponent(table, df.format(disp), isSelected, hasFocus, row, column);
				if (!isSelected)
				{
					c.setForeground(active ? table.getForeground() : java.awt.Color.GRAY);
				}
				setHorizontalAlignment(SwingConstants.RIGHT);
				return c;
			}
		};
		try
		{
			int splitIdx = metricsTable.getColumnModel().getColumnIndex("Split");
			metricsTable.getColumnModel().getColumn(splitIdx).setCellRenderer(splitRenderer);
		}
		catch (IllegalArgumentException ignored)
		{
		}

		try
		{
			int actionIdx = metricsTable.getColumnModel().getColumnIndex("X");
			metricsTable.getColumnModel().getColumn(actionIdx)
				.setCellEditor(new RemoveButtonEditor(this, sessionManager, metricsTable));
		}
		catch (IllegalArgumentException ignored)
		{
		}

		JComponent centerContent;
		if (direct)
		{
			Session currentSession = sessionManager.getCurrentSession().orElse(null);
			List<PlayerMetrics> data = sessionManager.computeMetricsFor(currentSession, true);
			List<Transfer> transfers = PaymentProcessor.computeDirectPaymentsStructured(data);

			if (transfers != null && !transfers.isEmpty())
			{
				javax.swing.table.DefaultTableModel txModel =
					new javax.swing.table.DefaultTableModel(new Object[]{"Suggested direct payments"}, 0)
					{
						@Override
						public boolean isCellEditable(int r, int c)
						{
							return false;
						}
					};

				for (Transfer t : transfers)
				{
					String payerShort = shortenName(t.getFrom(), 7);
					String payeeShort = shortenName(t.getTo(), 7);
					String amountStr = toSuffixString(Math.abs(t.getAmount()), config.defaultValueMultiplier().getValue());
					String display = payerShort + " -> " + payeeShort + ": " + amountStr;
					txModel.addRow(new Object[]{display});
				}

				JTable txTable = new JTable(txModel);
				txTable.setFillsViewportHeight(true);
				txTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
				txTable.setRowSelectionAllowed(false);
				txTable.setShowGrid(false);

				JScrollPane txScroll = new JScrollPane(txTable);
				txScroll.setPreferredSize(ll);
				centerContent = txScroll;
			}
			else
			{
				JScrollPane tableScroll = new JScrollPane(metricsTable);
				tableScroll.setPreferredSize(ll);
				centerContent = tableScroll;
			}
		}
		else
		{
			JScrollPane tableScroll = new JScrollPane(metricsTable);
			tableScroll.setPreferredSize(ll);
			centerContent = tableScroll;
		}

		wrapper.add(centerContent, BorderLayout.CENTER);

		JPanel btns = new JPanel(new GridLayout(1, 2, 6, 0));
		btns.add(copyBtn);
		btns.add(copyMdBtn);
		wrapper.add(btns, BorderLayout.SOUTH);

		return new DropdownRip("Settlement information", wrapper);
	}

	private void refreshMetrics()
	{
		Session currentSession = sessionManager.getCurrentSession().orElse(null);
		((Metrics) metricsTable.getModel()).setData(sessionManager.computeMetricsFor(currentSession, true));
	}

	private void copyMetricsJsonToClipboard()
	{
		String payload = MarkdownFormatter.buildMetricsJson(sessionManager);
		StringSelection selection = new StringSelection(payload);
		java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
	}

	private void copyMetricsMarkdownToClipboard()
	{
		String payload = MarkdownFormatter.buildMetricsMarkdown(
			sessionManager.computeMetricsFor(
				sessionManager.getCurrentSession().orElse(null), true), config);
		StringSelection selection = new StringSelection(payload);
		java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
	}

	public void refreshActivePlayerButtons()
	{
		activePlayersButtonsPanel.removeAll();
		Session current = sessionManager.getCurrentSession().orElse(null);
		if (current != null && current.isActive())
		{
			for (String player : new ArrayList<>(current.getPlayers()))
			{
				JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
				JLabel name = new JLabel(player);
				JButton addBtn = new JButton("➕");
				addBtn.setToolTipText("Add split amount to " + player);
				JButton rmBtn = new JButton("🗑");
				rmBtn.setToolTipText("Remove " + player + " from session");

				addBtn.addActionListener(e -> actions.altPlayerManageAddPlayer(player));
				rmBtn.addActionListener(e -> actions.altPlayerManageRemovePlayer(player));

				row.add(addBtn);
				row.add(rmBtn);
				row.add(name);
				activePlayersButtonsPanel.add(row, 0);
			}
		}
		else
		{
			JLabel hint = new JLabel("Start a session to manage players.");
			hint.setForeground(Color.GRAY);
			activePlayersButtonsPanel.add(hint);
		}
		activePlayersButtonsPanel.revalidate();
		activePlayersButtonsPanel.repaint();
	}
}
