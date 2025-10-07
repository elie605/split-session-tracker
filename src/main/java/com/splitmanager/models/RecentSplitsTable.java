package com.splitmanager.models;


import lombok.Setter;

public final class RecentSplitsTable extends javax.swing.table.AbstractTableModel {
    private final java.util.List<Row> rows = new java.util.ArrayList<>(10);
    private static final String[] COLS = {"Time", "Player", "Amount"};

    private static final java.time.format.DateTimeFormatter TIME_FMT =
            java.time.format.DateTimeFormatter
                    .ofLocalizedTime(java.time.format.FormatStyle.SHORT)
                    .withLocale(java.util.Locale.getDefault());

    private static final java.time.ZoneId SYS_TZ = java.time.ZoneId.systemDefault();

    public interface Listener {
        void onEdited();
    }

    @Setter
    private Listener listener;

    private static final class Row {
        final Kill kill; // keep reference for editing
        final String time;

        Row(Kill kill, String time) {
            this.kill = kill;
            this.time = time;
        }
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    @Override
    public String getColumnName(int column) {
        return COLS[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 1 || columnIndex == 2; // player, amount
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Row e = rows.get(rowIndex);
        switch (columnIndex) {
            case 0:
                return e.time;
            case 1:
                return e.kill.getPlayer();
            case 2:
                return (e.kill.getAmount() + "K");
            default:
                return "";
        }
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (rowIndex < 0 || rowIndex >= rows.size()) return;
        Row e = rows.get(rowIndex);
        if (columnIndex == 1) { // player
            String v = aValue == null ? null : aValue.toString();
            if (v != null && !v.isBlank()) {
                e.kill.setPlayer(v.trim());
            }
        } else if (columnIndex == 2) { // amount (K)
            try {
                String s = aValue == null ? "" : aValue.toString().trim().toLowerCase();
                if (s.endsWith("k")) s = s.substring(0, s.length() - 1);
                s = s.replace(",", "");
                long k = Long.parseLong(s);
                e.kill.setAmount(k);
            } catch (Exception ignored) {
            }
        }
        fireTableRowsUpdated(rowIndex, rowIndex);
        if (listener != null) listener.onEdited();
    }

    private void addEntry(Kill k) {
        String timeStr = "";
        if (k.getAt() != null) {
            timeStr = TIME_FMT.format(java.time.ZonedDateTime.ofInstant(k.getAt(), SYS_TZ));
        }
        // newest on top
        rows.add(0, new Row(k, timeStr));
        fireTableDataChanged();
    }

    public void setFromKills(java.util.List<Kill> kills) {
        clear();
        if (kills == null || kills.isEmpty()) {
            fireTableDataChanged();
            return;
        }
        int n = kills.size();
        // Iterate from newest to oldest
        for (int i = n - 1; i >= 0; i--) {
            Kill k = kills.get(i);
            addEntry(k);
        }
        fireTableDataChanged();
    }

    public void clear() {
        rows.clear();
        fireTableDataChanged();
    }
}
