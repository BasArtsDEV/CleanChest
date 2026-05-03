package dev.basarts.cleanchest.Utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;

public class UpdateChecker {

    private final JavaPlugin javaPlugin;
    private final String localPluginVersion;
    private String spigotPluginVersion;

    private static final int ID = 101616;
    private static final String ERR_MSG = "&cUpdate checker failed!";
    private static final String UPDATE_MSG = "&6A new update is available at: https://www.spigotmc.org/resources/" + ID + "/updates";
    private static final Permission UPDATE_PERM = new Permission("yourplugin.update", PermissionDefault.TRUE);
    private static final long CHECK_INTERVAL = 12_000; //In ticks.

    public UpdateChecker(final JavaPlugin javaPlugin) {
        this.javaPlugin = javaPlugin;
        this.localPluginVersion = javaPlugin.getPluginMeta().getVersion();
    }

    public void checkForUpdate() {
        new BukkitRunnable() {
            @Override
            public void run() {
                //The request is executed asynchronously as to not block the main thread.
                Bukkit.getScheduler().runTaskAsynchronously(javaPlugin, () -> {
                    //Request the current version of your plugin on SpigotMC.
                    try {
                        URL url = URI.create("https://api.spigotmc.org/legacy/update.php?resource=" + ID).toURL();
                        final HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

                        connection.setRequestMethod("GET");
                        spigotPluginVersion = new BufferedReader(new InputStreamReader(connection.getInputStream())).readLine();
                    } catch (final IOException e) {
                        Bukkit.getServer().getConsoleSender().sendMessage(MessageUtils.translateAlternateColorCodes('&', ERR_MSG));
                        javaPlugin.getLogger().severe("" + e);
                        cancel();
                        return;
                    }

                    //Check if the requested version is the same as the one in your plugin.yml.
                    if (localPluginVersion.equals(spigotPluginVersion)) return;

                    Bukkit.getServer().getConsoleSender().sendMessage(MessageUtils.translateAlternateColorCodes('&', UPDATE_MSG));

                    //Register the PlayerJoinEvent
                    Bukkit.getScheduler().runTask(javaPlugin, () -> Bukkit.getPluginManager().registerEvents(new Listener() {
                        @EventHandler(priority = EventPriority.MONITOR)
                        public void onPlayerJoin(final PlayerJoinEvent event) {
                            final Player player = event.getPlayer();
                            if (!player.hasPermission(UPDATE_PERM)) return;
                            sendUpdateMessage(player);
                        }
                    }, javaPlugin));

                    cancel(); //Cancel the runnable as an update has been found.
                });
            }
        }.runTaskTimer(javaPlugin, 0, CHECK_INTERVAL);
    }

    private void sendUpdateMessage(Player player) {
        Component line = Component.text("--------------------------------------").color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.STRIKETHROUGH, true);

        Component header = Component.text("✨ NEW UPDATE AVAILABLE ✨").color(NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.STRIKETHROUGH, false);

        Component versions = Component.text("Current version : ").color(NamedTextColor.GRAY)
                .append(Component.text(localPluginVersion).color(NamedTextColor.RED))
                .append(Component.newline())
                .append(Component.text("New version : ").color(NamedTextColor.GRAY))
                .append(Component.text(spigotPluginVersion).color(NamedTextColor.GREEN));

        Component link = Component.text("➜ ").color(NamedTextColor.YELLOW)
                .append(Component.text("[Click here to download]")
                        .color(NamedTextColor.AQUA)
                        .decoration(TextDecoration.UNDERLINED, true)
                        .clickEvent(ClickEvent.openUrl("https://www.spigotmc.org/resources/" + ID + "/updates"))
                        .hoverEvent(HoverEvent.showText(Component.text("Open in SpigotMC").color(NamedTextColor.GRAY))));

        player.sendMessage(line);
        player.sendMessage(header);
        player.sendMessage(Component.empty());
        player.sendMessage(versions);
        player.sendMessage(Component.empty());
        player.sendMessage(link);
        player.sendMessage(line);
    }
}
