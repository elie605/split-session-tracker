package com.example.pksession.panel;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;

public class TableRemoveButtonRenderer extends JButton implements TableCellRenderer {
    TableRemoveButtonRenderer() {
        setOpaque(true);
    }
    @Override
    public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        setText("X");
        return this;
    }
}

