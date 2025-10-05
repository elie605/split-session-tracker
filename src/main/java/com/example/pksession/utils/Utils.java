package com.example.pksession.utils;

import com.example.pksession.ManagerPanel;
import com.example.pksession.ManagerPlugin;

import javax.swing.*;
import java.awt.*;

public class Utils {
    public static void toast(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg);
    }

    public static Runnable requestUiRefresh()
    {
        // Capture current panel (may be null during startup/shutdown)
        ManagerPanel panel = ManagerPlugin.getPanel();
        // Also schedule a safe refresh on the EDT, rechecking for null
        SwingUtilities.invokeLater(() -> {
            ManagerPanel p = ManagerPlugin.getPanel();
            if (p != null)
            {
                p.refreshAllView();
            }
        });
        // Return a no-op if panel is null to avoid NPE when callers `.run()` it
        return panel != null ? panel::refreshAllView : () -> {};
    }
}
