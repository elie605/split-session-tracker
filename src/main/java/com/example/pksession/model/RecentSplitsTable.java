package com.example.pksession.model;


public final class RecentSplitsTable extends javax.swing.table.AbstractTableModel {
    private final java.util.List<Row> rows = new java.util.ArrayList<>(10);
    private static final String[] COLS = {"Time", "Player", "Amount"};

    private static final java.time.format.DateTimeFormatter TIME_FMT =
            java.time.format.DateTimeFormatter
                    .ofLocalizedTime(java.time.format.FormatStyle.SHORT)
                    .withLocale(java.util.Locale.getDefault());

    private static final java.time.ZoneId SYS_TZ = java.time.ZoneId.systemDefault();

    private static final class Row {
        final String time;
        final String player;
        final long amountK;

        Row(String time, String player, long amountK) {
            this.time = time;
            this.player = player;
            this.amountK = amountK;
        }
    }

    @Override public int getRowCount() { return rows.size(); }
    @Override public int getColumnCount() { return 3; }
    @Override public String getColumnName(int column) { return COLS[column]; }
    @Override public Class<?> getColumnClass(int columnIndex) { return String.class; }
    @Override public boolean isCellEditable(int r, int c) { return false; }

    @Override public Object getValueAt(int rowIndex, int columnIndex) {
        Row e = rows.get(rowIndex);
        switch (columnIndex) {
            case 0: return e.time;
            case 1: return e.player;
            case 2: return (e.amountK + "K");
            default: return "";
        }
    }

    private void addEntry(String player, long amountK, java.time.Instant at) {
        String timeStr = "";
        if (at != null) {
            timeStr = TIME_FMT.format(java.time.ZonedDateTime.ofInstant(at, SYS_TZ));
        }
        // newest on top
        rows.add(0, new Row(timeStr, player, amountK));
        fireTableDataChanged();
    }

    public void setFromKills(java.util.List<com.example.pksession.model.Kill> kills) {
        clear();
        if (kills == null || kills.isEmpty()) {
            fireTableDataChanged();
            return;
        }
        int n = kills.size();
        // Iterate from newest to oldest
        for (int i = n - 1; i >= 0; i--) {
            com.example.pksession.model.Kill k = kills.get(i);
            long amountK = (long) Math.floor(k.getAmount());
            addEntry(k.getPlayer(), amountK, k.getAt());
        }
        fireTableDataChanged();
    }
    public void clear() {
        rows.clear();
        fireTableDataChanged();
    }
}
