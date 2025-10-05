package com.example.pksession.views.components.table;

import com.example.pksession.ManagerSession;

import javax.swing.*;
import java.awt.*;

import static com.example.pksession.utils.Utils.requestUiRefresh;
import static com.example.pksession.utils.Utils.toast;

public class RemoveButtonEditor extends DefaultCellEditor {
    private final JButton button = new JButton("X");
    private int row = -1;


    public RemoveButtonEditor(Component parent, ManagerSession manager, JTable metricsTable) {
        super(new JCheckBox());

        button.setOpaque(true);
        button.addActionListener(e -> {
            if (row >= 0 && !manager.isHistoryLoaded()) {
                String player = (String) metricsTable.getModel().getValueAt(row, 0);
                if (manager.removePlayerFromSession(player)) {
                    requestUiRefresh().run();
                } else {
                    toast(parent, "Failed to remove player.");
                }
            }
            fireEditingStopped();
        });

    }


    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        this.row = row;
        return button;
    }

    @Override
    public Object getCellEditorValue() {
        return "X";
    }

    @Override
    public boolean stopCellEditing() {
        this.row = -1;
        return super.stopCellEditing();
    }

}
