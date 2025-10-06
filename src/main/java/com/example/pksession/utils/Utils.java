package com.example.pksession.utils;

import ch.qos.logback.classic.Logger;
import com.example.pksession.ManagerPanel;
import com.example.pksession.ManagerPlugin;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
@Slf4j
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

    public static Runnable requestRestart()
    {
        // Capture current panel (may be null during startup/shutdown)
        ManagerPanel panel = ManagerPlugin.getPanel();
        // Also schedule a safe refresh on the EDT, rechecking for null
        SwingUtilities.invokeLater(() -> {
            ManagerPanel p = ManagerPlugin.getPanel();
            if (p != null)
            {
                p.restart();
            }
        });
        // Return a no-op if panel is null to avoid NPE when callers `.run()` it
        return panel != null ? panel::restart : () -> {};
    }
}
