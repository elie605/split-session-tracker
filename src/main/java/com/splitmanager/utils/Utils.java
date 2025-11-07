package com.splitmanager.utils;

import java.awt.Component;
import javax.swing.JOptionPane;

public class Utils
{
	public static void toast(Component parent, String msg)
	{
		JOptionPane.showMessageDialog(parent, msg);
	}
}
