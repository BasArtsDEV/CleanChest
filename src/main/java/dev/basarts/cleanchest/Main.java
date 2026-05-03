package dev.basarts.cleanchest;

import dev.basarts.cleanchest.Commands.CleanChestCMD;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {
    private static Main instance;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        instance = this;

        getCommand("cleanchest").setExecutor(new CleanChestCMD());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static Main getInstance() {
        return instance;
    }
}
