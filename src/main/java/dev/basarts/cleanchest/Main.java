package dev.basarts.cleanchest;

import dev.basarts.cleanchest.Commands.CleanChestCMD;
import dev.basarts.cleanchest.Utils.UpdateChecker;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {
    private static Main instance;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        instance = this;

        getCommand("cleanchest").setExecutor(new CleanChestCMD());

        new UpdateChecker(this).checkForUpdate();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static Main getInstance() {
        return instance;
    }
}
