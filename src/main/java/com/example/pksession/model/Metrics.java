package com.example.pksession.model;

import com.example.pksession.Formats;
import com.example.pksession.SessionManager;

import javax.swing.table.AbstractTableModel;
import java.util.List;
import java.util.Comparator;
import java.util.stream.Collectors;

public final class Metrics extends AbstractTableModel {
    private List<SessionManager.PlayerMetrics> rows = List.of();

    public void setData(List<SessionManager.PlayerMetrics> rows) {
        // Sort: active first, inactive at bottom; stable within groups
        this.rows = rows.stream()
                .sorted(Comparator.comparingInt(pm -> pm.activePlayer ? 0 : 1)) // active=true → 0, inactive=false → 1
                .collect(Collectors.toList());
        fireTableDataChanged();
    }

    // Helper for renderers/editors to know if a row is active
    public boolean isRowActive(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= rows.size()) return false;
        return rows.get(rowIndex).activePlayer;
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
        // Use Object so we can render either a JButton (active) or a JLabel (inactive) in col 3
        return Object.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        // Only the action column is potentially editable, and only for active players
        return columnIndex == 3 && isRowActive(rowIndex);
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
                // Show X for active players, sleeping emoji for non-active
                return r.activePlayer ? "X" : "💤";
            default:
                return "";
        }
    }
}