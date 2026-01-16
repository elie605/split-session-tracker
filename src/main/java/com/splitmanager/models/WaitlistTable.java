package com.splitmanager.models;

import static com.splitmanager.utils.Formats.OsrsAmountFormatter.toSuffixString;
import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;

public class WaitlistTable extends AbstractTableModel
{
	private final String[] cols = {"Type", "Value", "Player"};
	private final List<PendingValue> rows = new ArrayList<>();

	public void setData(List<PendingValue> pending)
	{
		rows.clear();
		if (pending != null)
		{
			rows.addAll(pending);
		}
		fireTableDataChanged();
	}

	public PendingValue getRow(int idx)
	{
		if (idx < 0 || idx >= rows.size())
		{
			return null;
		}
		return rows.get(idx);
	}

	@Override
	public int getRowCount()
	{
		return rows.size();
	}

	@Override
	public int getColumnCount()
	{
		return cols.length;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex)
	{
		PendingValue pv = rows.get(rowIndex);
		switch (columnIndex)
		{
			case 0:
				return pv.getType().name();
			case 1:
				long value = pv.getValue();
				//TODO get this from config
				return toSuffixString(value, "k");
			case 2:
				return pv.getSuggestedPlayer() == null ? "" : pv.getSuggestedPlayer();
		}
		return null;
	}

	@Override
	public String getColumnName(int column)
	{
		return cols[column];
	}

	@Override
	public Class<?> getColumnClass(int columnIndex)
	{
		switch (columnIndex)
		{
			case 0:
			case 2:
				return String.class;
			case 1:
				return String.class; // formatted K string
		}
		return Object.class;
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex)
	{
		return columnIndex == 2;
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex)
	{
		if (columnIndex != 2)
		{
			return;
		}
		PendingValue pv = rows.get(rowIndex);
		pv.setSuggestedPlayer(aValue == null ? null : aValue.toString());
	}
}
