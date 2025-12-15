package com.splitmanager.models;

import com.splitmanager.utils.Formats;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.table.AbstractTableModel;

public final class Metrics extends AbstractTableModel
{
	private List<PlayerMetrics> rows = List.of();
	private boolean hideTotalColumn = false;

	public void setHideTotalColumn(boolean hide)
	{
		if (this.hideTotalColumn != hide)
		{
			this.hideTotalColumn = hide;
			fireTableStructureChanged();
		}
	}

	public boolean isHidingTotalColumn()
	{
		return hideTotalColumn;
	}

	public void setData(List<PlayerMetrics> rows)
	{
		// Sort: active first, inactive at bottom; stable within groups
		this.rows = rows.stream()
			.sorted(Comparator.comparingInt(pm -> pm.activePlayer ? 0 : 1)) // active=true → 0, inactive=false → 1
			.collect(Collectors.toList());
		fireTableDataChanged();
	}

	// Helper for renderers/editors to know if a row is active
	public boolean isRowActive(int rowIndex)
	{
		if (rowIndex < 0 || rowIndex >= rows.size())
		{
			return false;
		}
		return rows.get(rowIndex).activePlayer;
	}

	@Override
	public int getRowCount()
	{
		return rows.size();
	}

	@Override
	public int getColumnCount()
	{
		return hideTotalColumn ? 3 : 4;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex)
	{
		var r = rows.get(rowIndex);
		if (!hideTotalColumn)
		{
			switch (columnIndex)
			{
				case 0:
					return r.player;
				case 1:
					return Formats.OsrsAmountFormatter.toSuffixString(r.total,'k');
				case 2:
					return Formats.OsrsAmountFormatter.toSuffixString(r.split,'k');
				case 3:
					// Show X for active players, sleeping emoji for non-active
					return r.activePlayer ? "X" : "💤";
				default:
					return "";
			}
		}
		else
		{
			switch (columnIndex)
			{
				case 0:
					return r.player;
				case 1:
					return Formats.OsrsAmountFormatter.toSuffixString(r.split,'k');
				case 2:
					return r.activePlayer ? "X" : "💤";
				default:
					return "";
			}
		}
	}

	@Override
	public String getColumnName(int column)
	{
		if (!hideTotalColumn)
		{
			switch (column)
			{
				case 0:
					return "Player";
				case 1:
					return "Total";
				case 2:
					return "Split";
				case 3:
					return "X";
				default:
					return "";
			}
		}
		else
		{
			switch (column)
			{
				case 0:
					return "Player";
				case 1:
					return "Split";
				case 2:
					return "X";
				default:
					return "";
			}
		}
	}

	@Override
	public Class<?> getColumnClass(int columnIndex)
	{
		// Use Object so we can render either a JButton (active) or a JLabel (inactive) in action col
		return Object.class;
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex)
	{
		// Only the action column is potentially editable, and only for active players
		int actionCol = hideTotalColumn ? 2 : 3;
		return columnIndex == actionCol && isRowActive(rowIndex);
	}

	// Accessor for renderers: raw split value for row
	public long getRawSplitAt(int rowIndex)
	{
		if (rowIndex < 0 || rowIndex >= rows.size())
		{
			return 0L;
		}
		return rows.get(rowIndex).split;
	}
}