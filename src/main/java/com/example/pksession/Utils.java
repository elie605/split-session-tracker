package com.example.pksession;

import javax.swing.*;
import java.awt.*;

public class Utils {
    public static void toast(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg);
    }

    public static Runnable requestUiRefresh()
    {
        PkSessionPanel panel = PkSessionPlugin.getPanel();
        SwingUtilities.invokeLater(() -> {
            if (panel != null)
            {
                panel.refreshAllView();
            }
        });
        return panel::refreshAllView;
    }
}
