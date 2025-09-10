package com.example;

import com.example.pksession.PkSessionPlugin;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class ExamplePluginTest
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(PkSessionPlugin.class);
        RuneLite.main(args);
    }
}