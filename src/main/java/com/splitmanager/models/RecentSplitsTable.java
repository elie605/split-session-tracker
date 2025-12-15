package com.splitmanager.models;


import com.splitmanager.ManagerPlugin;
import com.splitmanager.PluginConfig;
import com.splitmanager.utils.Formats;
import lombok.Setter;

public final class RecentSplitsTable extends javax.swing.table.AbstractTableModel
{
	private static final String[] COLS = {"Time", "Player", "Amount"};
	private static final java.time.format.DateTimeFormatter TIME_FMT =
		java.time.format.DateTimeFormatter
			.ofLocalizedTime(java.time.format.FormatStyle.SHORT)
			.withLocale(java.util.Locale.getDefault());
	private static final java.time.ZoneId SYS_TZ = java.time.ZoneId.systemDefault();
	private final java.util.List<Row> rows = new java.util.ArrayList<>(10);
	@Setter
	private Listener listener;

	private final PluginConfig config;

	public RecentSplitsTable(PluginConfig config)
	{
		this.config = config;
	}

	@Override
	public int getRowCount()
	{
		return rows.size();
	}

	@Override
	public int getColumnCount()
	{
		return 3;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex)
	{
		Row e = rows.get(rowIndex);
		switch (columnIndex)
		{
			case 0:
				return e.time;
			case 1:
				return e.kill.getPlayer();
			case 2:
				return Formats.OsrsAmountFormatter.toSuffixString(e.kill.getAmount(), 'k');
			default:
				return "";
		}
	}

	@Override
	public String getColumnName(int column)
	{
		return COLS[column];
	}

	@Override
	public Class<?> getColumnClass(int columnIndex)
	{
		return String.class;
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex)
	{
		return columnIndex == 1 || columnIndex == 2; // player, amount
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex)
	{
		if (rowIndex < 0 || rowIndex >= rows.size())
		{
			return;
		}
		Row e = rows.get(rowIndex);
		if (columnIndex == 1)
		{ // player
			String v = aValue == null ? null : aValue.toString();
			if (v != null && !v.isBlank())
			{
				e.kill.setPlayer(v.trim());
			}
		}
		else if (columnIndex == 2)
		{ // amount (K)
			try
			{
				String valueStr = (String) aValue;

				// Check if the value has no unit (k, m, b) and append the default
				java.util.regex.Pattern unitPattern = java.util.regex.Pattern.compile("(?i)^\\s*([0-9][0-9,]*(?:\\.[0-9]+)?)\\s*([kmb])?\\s*$");
				java.util.regex.Matcher matcher = unitPattern.matcher(valueStr);

				if (matcher.matches()) {
					String numberTxt = matcher.group(1);
					String unitTxt = matcher.group(2);

					if (unitTxt == null) {
						// No unit specified, append the default multiplier
						valueStr = numberTxt + config.defaultValueMultiplier().getValue();
					}
				}

				Object k = new Formats.OsrsAmountFormatter().stringToValue(valueStr);
				if (k == null)
				{
					return;
				}
				e.kill.setAmount((Long) k);
			}
			catch (Exception ignored)
			{
			}
		}
		fireTableRowsUpdated(rowIndex, rowIndex);
		if (listener != null)
		{
			listener.onEdited();
		}
	}

	private void addEntry(Kill k)
	{
		String timeStr = "";
		if (k.getAt() != null)
		{
			timeStr = TIME_FMT.format(java.time.ZonedDateTime.ofInstant(k.getAt(), SYS_TZ));
		}
		// newest on top
		rows.add(0, new Row(k, timeStr));
		fireTableDataChanged();
	}

	public void setFromKills(java.util.List<Kill> kills)
	{
		clear();
		if (kills == null || kills.isEmpty())
		{
			fireTableDataChanged();
			return;
		}
		int n = kills.size();
		// Iterate from newest to oldest
		for (int i = n - 1; i >= 0; i--)
		{
			Kill k = kills.get(i);
			addEntry(k);
		}
		fireTableDataChanged();
	}

	public void clear()
	{
		rows.clear();
		fireTableDataChanged();
	}

	public interface Listener
	{
		void onEdited();
	}

	private static final class Row
	{
		final Kill kill; // keep reference for editing
		final String time;

		Row(Kill kill, String time)
		{
			this.kill = kill;
			this.time = time;
		}
	}
}
