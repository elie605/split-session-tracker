package com.example.pksession.controllers;

import com.example.pksession.utils.Formats;
import com.example.pksession.PluginConfig;
import com.example.pksession.ManagerSession;
import com.example.pksession.models.Transfer;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for PanelView: contains non-UI logic such as computing transfers and
 * generating export strings. This helps keep PanelView focused on view concerns.
 */
public class PanelController {
    private final ManagerSession manager;
    private final PluginConfig config;

    public PanelController(ManagerSession manager, PluginConfig config) {
        this.manager = manager;
        this.config = config;
    }

    public String buildMetricsJson() {
        var currentSession = manager.getCurrentSession().orElse(null);
        List<ManagerSession.PlayerMetrics> data = manager.computeMetricsFor(currentSession, true);
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < data.size(); i++) {
            var pm = data.get(i);
            sb.append("{\"player\":\"").append(pm.player).append("\",")
              .append("\"total\":").append(pm.total).append(",")
              .append("\"split\":").append(pm.split).append(",")
              .append("\"active\":").append(pm.activePlayer).append("}");
            if (i < data.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    public String buildMetricsMarkdown() {
        var currentSession = manager.getCurrentSession().orElse(null);
        List<ManagerSession.PlayerMetrics> data = manager.computeMetricsFor(currentSession, true);
        DecimalFormat df = Formats.getDecimalFormat();

        boolean directMode = config.directPayments();
        boolean forDiscord = config.copyForDiscord();

        StringBuilder sb = new StringBuilder();
        if (forDiscord) sb.append("```\n");

        if (!directMode) {
            List<String[]> rows = new ArrayList<>();
            int maxPlayer = "Player".length();
            int maxTotal = "Total".length();
            int maxSplit = "Split".length();
            for (var pm : data) {
                String player = pm.player == null ? "" : pm.player.replace("|", "\\|");
                String total = df.format(pm.total);
                long dispSplit = pm.split;
                if (config.flipSettlementSign()) dispSplit = -dispSplit;
                String split = df.format(dispSplit);
                rows.add(new String[]{player, total, split});
                if (player.length() > maxPlayer) maxPlayer = player.length();
                if (total.length() > maxTotal) maxTotal = total.length();
                if (split.length() > maxSplit) maxSplit = split.length();
            }
            sb.append("| ")
              .append(padRight("Player", maxPlayer)).append(" | ")
              .append(padLeft("Total", maxTotal)).append(" | ")
              .append(padLeft("Split", maxSplit)).append(" |\n");
            sb.append("| ")
              .append(repeat('-', maxPlayer)).append(" | ")
              .append(repeat('-', maxTotal - 1)).append(":").append(" | ")
              .append(repeat('-', maxSplit - 1)).append(":").append(" |\n");
            for (String[] r : rows) {
                sb.append("| ")
                  .append(padRight(r[0], maxPlayer)).append(" | ")
                  .append(padLeft(r[1], maxTotal)).append(" | ")
                  .append(padLeft(r[2], maxSplit)).append(" |\n");
            }
        } else {
            List<String[]> rows = new ArrayList<>();
            int maxPlayer = "Player".length();
            int maxSplit = "Split".length();
            for (var pm : data) {
                String player = pm.player == null ? "" : pm.player.replace("|", "\\|");
                long dispSplit = pm.split; // do not flip in direct mode
                String split = df.format(dispSplit);
                rows.add(new String[]{player, split});
                if (player.length() > maxPlayer) maxPlayer = player.length();
                if (split.length() > maxSplit) maxSplit = split.length();
            }
            sb.append("| ")
              .append(padRight("Player", maxPlayer)).append(" | ")
              .append(padLeft("Split", maxSplit)).append(" |\n");
            sb.append("| ")
              .append(repeat('-', maxPlayer)).append(" | ")
              .append(repeat('-', maxSplit - 1)).append(":").append(" |\n");
            for (String[] r : rows) {
                sb.append("| ")
                  .append(padRight(r[0], maxPlayer)).append(" | ")
                  .append(padLeft(r[1], maxSplit)).append(" |\n");
            }
        }

        if (config.directPayments()) {
            List<String> transfers = computeDirectPayments(data);
            if (!transfers.isEmpty()) {
                sb.append('\n').append("Suggested direct payments:\n");
                for (String line : transfers) sb.append("- ").append(line).append('\n');
            }
        }

        if (forDiscord) sb.append("```\n");
        return sb.toString();
    }

    public List<String> computeDirectPayments(List<ManagerSession.PlayerMetrics> data) {
        List<ManagerSession.PlayerMetrics> receivers = new ArrayList<>();
        List<ManagerSession.PlayerMetrics> payers = new ArrayList<>();
        for (var pm : data) {
            if (pm.split > 0) receivers.add(pm);
            else if (pm.split < 0) payers.add(pm);
        }
        receivers.sort((a, b) -> Long.compare(b.split, a.split));
        payers.sort((a, b) -> Long.compare(Math.abs(b.split), Math.abs(a.split)));

        List<String> lines = new ArrayList<>();
        DecimalFormat df = Formats.getDecimalFormat();

        int i = 0, j = 0;
        long recvLeft = receivers.isEmpty() ? 0 : receivers.get(0).split;
        long payLeft = payers.isEmpty() ? 0 : -payers.get(0).split; // make positive
        while (i < receivers.size() && j < payers.size()) {
            long amt = Math.min(recvLeft, payLeft);
            if (amt > 0) {
                String from = payers.get(j).player;
                String to = receivers.get(i).player;
                lines.add(from + " -> " + to + ": " + df.format(amt));
                recvLeft -= amt;
                payLeft -= amt;
            }
            if (recvLeft == 0) {
                i++;
                if (i < receivers.size()) recvLeft = receivers.get(i).split;
            }
            if (payLeft == 0) {
                j++;
                if (j < payers.size()) payLeft = -payers.get(j).split;
            }
        }
        return lines;
    }

    public List<Transfer> computeDirectPaymentsStructured(List<ManagerSession.PlayerMetrics> data) {
        List<ManagerSession.PlayerMetrics> receivers = new ArrayList<>();
        List<ManagerSession.PlayerMetrics> payers = new ArrayList<>();
        for (ManagerSession.PlayerMetrics pm : data) {
            if (pm.split > 0) receivers.add(pm);
            else if (pm.split < 0) payers.add(pm);
        }
        receivers.sort((a, b) -> Long.compare(b.split, a.split));
        payers.sort((a, b) -> Long.compare(Math.abs(b.split), Math.abs(a.split)));

        List<Transfer> out = new ArrayList<>();
        int i = 0, j = 0;
        long recvLeft = receivers.isEmpty() ? 0 : receivers.get(0).split;
        long payLeft = payers.isEmpty() ? 0 : -payers.get(0).split; // make positive

        while (i < receivers.size() && j < payers.size()) {
            long amt = Math.min(recvLeft, payLeft);
            if (amt > 0) {
                String from = payers.get(j).player;
                String to = receivers.get(i).player;
                out.add(new Transfer(from, to, amt));
                recvLeft -= amt;
                payLeft -= amt;
            }
            if (recvLeft == 0) {
                i++;
                if (i < receivers.size()) recvLeft = receivers.get(i).split;
            }
            if (payLeft == 0) {
                j++;
                if (j < payers.size()) payLeft = -payers.get(j).split;
            }
        }
        return out;
    }

    private static String padRight(String s, int width) {
        if (s == null) s = "";
        if (s.length() >= width) return s;
        StringBuilder sb = new StringBuilder(width);
        sb.append(s);
        for (int i = s.length(); i < width; i++) sb.append(' ');
        return sb.toString();
    }

    private static String padLeft(String s, int width) {
        if (s == null) s = "";
        StringBuilder sb = new StringBuilder(width);
        for (int i = s.length(); i < width; i++) sb.append(' ');
        sb.append(s);
        return sb.toString();
    }

    private static String repeat(char ch, int count) {
        if (count <= 0) return "";
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) sb.append(ch);
        return sb.toString();
    }
}
