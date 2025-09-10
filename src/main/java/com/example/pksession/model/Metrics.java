package com.example.pksession.model;

import com.example.pksession.Formats;
import com.example.pksession.SessionManager;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.util.List;

public final class Metrics extends AbstractTableModel {
    private Session session;
    private List<SessionManager.PlayerMetrics> rows = List.of();

    public void setData(Session session, List<SessionManager.PlayerMetrics> rows) {
        this.session = session;
        this.rows = rows;
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return 4;
    }

    @Override
    public String getColumnName(int column) {
        switch (column) {
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

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == 3) {
            return JButton.class;
        }
        return Object.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 3;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        var r = rows.get(rowIndex);
        switch (columnIndex) {
            case 0:
                return r.player;
            case 1:
                return Formats.getDecimalFormat().format(r.total);
            case 2:
                return Formats.getDecimalFormat().format(r.split);
            case 3:
                return "X";
            default:
                return "";
        }
    }
}

