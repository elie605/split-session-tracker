package com.splitmanager.views.components.table;

import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

public class RemoveButtonRenderer extends JButton implements TableCellRenderer
{
	RemoveButtonRenderer()
	{
		setOpaque(true);
	}

	@Override
	public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
	{
		setText("X");
		return this;
	}
}

