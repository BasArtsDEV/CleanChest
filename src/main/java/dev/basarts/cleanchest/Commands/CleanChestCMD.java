package dev.basarts.cleanchest.Commands;

import dev.basarts.cleanchest.Main;
import dev.basarts.cleanchest.Utils.MessageUtils;
import org.bukkit.Chunk;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class CleanChestCMD implements CommandExecutor {

    private final Main plugin = Main.getInstance();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NonNull [] args) {
        if (!(sender instanceof Player player)) return true;

        String prefix = MessageUtils.format(plugin.getConfig().getString("messages.prefix"));

        if (!player.hasPermission("cleanchest.use")) {
            MessageUtils.sendMessage(player, prefix + MessageUtils.format(plugin.getConfig().getString("messages.no-permission")));
            player.playSound(player, Sound.BLOCK_CHEST_LOCKED, 1f, 1f);
            return true;
        }

        World world = player.getWorld();
        Chunk[] loadedChunks = world.getLoadedChunks();

        MessageUtils.sendMessage(player, prefix + MessageUtils.format(plugin.getConfig().getString("messages.start")));
        player.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);

        new BukkitRunnable() {
            int index = 0;
            int deletedCount = 0;
            final int chunksPerTick = plugin.getConfig().getInt("chunks-per-tick", 5);

            @Override
            public void run() {
                for (int i = 0; i < chunksPerTick; i++) {
                    if (index >= loadedChunks.length) {
                        String finished = MessageUtils.format(plugin.getConfig().getString("messages.finished"),
                                "%items%", String.valueOf(deletedCount),
                                "%chunks%", String.valueOf(loadedChunks.length));

                        MessageUtils.sendMessage(player, prefix + finished);
                        player.playSound(player, Sound.BLOCK_ANVIL_USE, 1f, 1f);

                        this.cancel();
                        return;
                    }

                    Chunk chunk = loadedChunks[index];
                    deletedCount += cleanChunk(chunk);
                    index++;
                }

                if (index % 20 == 0 || index == loadedChunks.length) {
                    double percent = (double) index / loadedChunks.length * 100;
                    String bar = MessageUtils.getProgressBar(index, loadedChunks.length, 20, "┃", "§a", "§7");

                    String progressMsg = MessageUtils.format(plugin.getConfig().getString("messages.progress"),
                            "%bar%", bar,
                            "%percent%", String.format("%.1f", percent),
                            "%current%", String.valueOf(index),
                            "%total%", String.valueOf(loadedChunks.length));

                    MessageUtils.sendActionBar(player, progressMsg);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return true;
    }

    private int cleanChunk(Chunk chunk) {
        int removed = 0;

        for (BlockState state : chunk.getTileEntities()) {
            if (state instanceof Container container) {
                removed += checkInventory(container.getInventory());
            }
        }

        if (plugin.getConfig().getBoolean("scan-entities")) {
            for (Entity entity : chunk.getEntities()) {
                switch (entity) {
                    case Item itemEnt -> {
                        if (isCheat(itemEnt.getItemStack())) {
                            entity.remove();
                            removed += itemEnt.getItemStack().getAmount();
                        }
                    }
                    case ArmorStand armorStand -> removed += checkEquipment(armorStand.getEquipment());
                    case ItemFrame itemFrame -> {
                        if (isCheat(itemFrame.getItem())) {
                            itemFrame.setItem(null);
                            removed++;
                        }
                    }
                    default -> {
                    }
                }
            }
        }
        return removed;
    }

    private int checkInventory(Inventory inv) {
        int count = 0;
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (isCheat(item)) {
                inv.setItem(i, null);
                count += item.getAmount();
            }
        }
        return count;
    }

    /**
     * Correction spécifique pour ArmorStand et équipements
     */
    private int checkEquipment(EntityEquipment equipment) {
        if (equipment == null) return 0;
        int count = 0;

        ItemStack[] armor = equipment.getArmorContents();
        for (int i = 0; i < armor.length; i++) {
            if (isCheat(armor[i])) {
                armor[i] = null;
                count++;
            }
        }
        equipment.setArmorContents(armor);

        if (isCheat(equipment.getItemInMainHand())) {
            equipment.setItemInMainHand(null);
            count++;
        }
        if (isCheat(equipment.getItemInOffHand())) {
            equipment.setItemInOffHand(null);
            count++;
        }

        return count;
    }

    private boolean isCheat(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;

        List<String> forbidden = plugin.getConfig().getStringList("forbidden-materials");
        if (forbidden.contains(item.getType().name())) return true;

        int maxLvl = plugin.getConfig().getInt("max-enchantment-level");
        if (item.hasItemMeta() && item.getItemMeta().hasEnchants()) {
            return item.getEnchantments().values().stream().anyMatch(lvl -> lvl > maxLvl);
        }

        return false;
    }
}