package com.huck.fakeplayer.main;

import com.huck.fakeplayer.command.FakePlayerCommand;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

public final class FakePlayerPlugin extends JavaPlugin {

    @Getter
    private static FakePlayerPlugin plugin;

    @Override
    public void onEnable() {
        plugin = this;

        // load
        saveDefaultConfig();

        loader();
    }

    @Override
    public void onDisable() {
        // destroy
        Bukkit.getScheduler().cancelTasks(this);
        HandlerList.unregisterAll(this);
    }

    protected void loader() {
        // init command
        new FakePlayerCommand();
    }
}
