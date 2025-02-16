package ch.lumarlie.railer;

import org.bukkit.plugin.java.JavaPlugin;

public final class Railer extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("Railer started");
        getServer().getPluginManager().registerEvents(new RailerEventListener(getLogger()), this);
    }

    @Override
    public void onDisable() {
        getLogger().info("Railer stoped");
    }

}
