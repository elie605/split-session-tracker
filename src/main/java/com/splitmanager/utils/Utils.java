package com.splitmanager.utils;

import com.splitmanager.ManagerPanel;
import com.splitmanager.ManagerPlugin;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;

public class Utils {
    public static void toast(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg);
    }
}
