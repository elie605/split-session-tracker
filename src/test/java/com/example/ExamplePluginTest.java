package com.example;

import com.splitmanager.ManagerPlugin;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class ExamplePluginTest
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(ManagerPlugin.class);
        RuneLite.main(args);
    }
}