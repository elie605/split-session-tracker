package com.example.pksession.model;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class WaitlistTableModel extends AbstractTableModel {
    private final String[] cols = {"Type", "Value", "Player"};
    private final List<PendingValue> rows = new ArrayList<>();
    private List<String> knownPlayers = new ArrayList<>();

    public void setData(List<PendingValue> pending, Set<String> knownPlayers) {
        rows.clear();
        if (pending != null) rows.addAll(pending);
        this.knownPlayers = new ArrayList<>(knownPlayers);
        fireTableDataChanged();
    }

    public PendingValue getRow(int idx) {
        if (idx < 0 || idx >= rows.size()) return null;
        return rows.get(idx);
    }

    public List<String> getKnownPlayers() {
        return knownPlayers;
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return cols.length;
    }

    @Override
    public String getColumnName(int column) {
        return cols[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case 0: return String.class;
            case 1: return String.class; // formatted K string
            case 2: return String.class;
        }
        return Object.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 2; // only suggested player editable
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        PendingValue pv = rows.get(rowIndex);
        switch (columnIndex) {
            case 0:
                return pv.getType().name();
            case 1:
                double k = pv.getValue();
                // show with max one decimal when needed
                String s = (k % 1.0 == 0.0) ? String.format("%,.0fK", k) : String.format("%,.1fK", k);
                return s;
            case 2:
                return pv.getSuggestedPlayer() == null ? "" : pv.getSuggestedPlayer();
        }
        return null;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (columnIndex != 2) return;
        PendingValue pv = rows.get(rowIndex);
        pv.setSuggestedPlayer(aValue == null ? null : aValue.toString());
    }
}
