package com.example.pksession.panel;

import com.example.pksession.SessionManager;

import javax.swing.*;
import java.awt.*;

import static com.example.pksession.Utils.requestUiRefresh;
import static com.example.pksession.Utils.toast;

public class TableRemoveButtonEditor extends DefaultCellEditor {
    private final JButton button = new JButton("X");
    private int row = -1;


    private final JTable metricsTable;
    private final Component parent;
    private final SessionManager manager;

    public TableRemoveButtonEditor(Component parent, SessionManager manager, JTable metricsTable) {
        super(new JCheckBox());
        this.manager = manager;
        this.metricsTable = metricsTable;
        this.parent = parent;

        button.setOpaque(true);
        button.addActionListener(e -> {
            if (row >= 0 && !manager.isHistoryLoaded()) {
                String player = (String) metricsTable.getModel().getValueAt(row, 0);
                if (manager.removePlayerFromSession(player)) {
                    requestUiRefresh();
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
