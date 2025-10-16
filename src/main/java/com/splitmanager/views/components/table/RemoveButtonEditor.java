package com.splitmanager.views.components.table;

import com.splitmanager.ManagerSession;
import static com.splitmanager.utils.Utils.requestUiRefresh;
import static com.splitmanager.utils.Utils.toast;
import java.awt.Component;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JTable;

/**
 * Table cell editor rendering a small X button to remove a player from the active session.
 */
public class RemoveButtonEditor extends DefaultCellEditor
{
	private final JButton button = new JButton("X");
	private int row = -1;


	/**
	 * Create the editor and wire the remove behavior.
	 *
	 * @param parent       parent component for toasts
	 * @param manager      session manager
	 * @param metricsTable table with player rows
	 */
	public RemoveButtonEditor(Component parent, ManagerSession manager, JTable metricsTable)
	{
		super(new JCheckBox());

		button.setOpaque(true);
		button.addActionListener(e -> {
			if (row >= 0 && !manager.isHistoryLoaded())
			{
				String player = (String) metricsTable.getModel().getValueAt(row, 0);
				if (manager.removePlayerFromSession(player))
				{
					requestUiRefresh().run();
				}
				else
				{
					toast(parent, "Failed to remove player.");
				}
			}
			fireEditingStopped();
		});

	}

	@Override
	public Object getCellEditorValue()
	{
		return "X";
	}

	@Override
	public boolean stopCellEditing()
	{
		this.row = -1;
		return super.stopCellEditing();
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column)
	{
		this.row = row;
		return button;
	}

}
