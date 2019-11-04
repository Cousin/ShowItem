package net.blancodev.showitem;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ShowItem extends JavaPlugin {

    private Map<UUID, Long> cooldowns;

    @Override
    public void onEnable() {

        this.cooldowns = new HashMap<>();

        // Register command
        getCommand("showitem").setExecutor((sender, command, label, args) -> {

            if (sender instanceof Player) {

                Player player = (Player) sender;
                if (player.hasPermission("showcase.show")) {

                    ItemStack itemStack = player.getItemInHand();
                    if (itemStack != null && itemStack.getType() != Material.AIR) {

                        // Check cooldown restrictions
                        if (player.hasPermission("showcase.cooldown") ||
                                System.currentTimeMillis() - cooldowns.getOrDefault(player.getUniqueId(), 0L) > 60_000) {

                            cooldowns.remove(player.getUniqueId());

                            // Convert ItemStack to JSON
                            net.minecraft.server.v1_8_R3.ItemStack craftItemStack = CraftItemStack.asNMSCopy(itemStack);
                            NBTTagCompound compound = new NBTTagCompound();
                            craftItemStack.save(compound);

                            String json = compound.toString();

                            String itemName = getItemName(itemStack);

                            // Simulate chat event to get the chat format
                            AsyncPlayerChatEvent chatEvent = new AsyncPlayerChatEvent(
                                    false, player, "TO_REPLACE_XX", Collections.emptySet()
                            );

                            getServer().getPluginManager().callEvent(chatEvent);

                            // Create the JSON chat message
                            TextComponent textComponent = new TextComponent(
                                    String.format(chatEvent.getFormat(), player.getDisplayName(), ChatColor.WHITE + "[" + chatEvent.getMessage().replace("TO_REPLACE_XX", itemName)) + ChatColor.WHITE + "]");

                            textComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new BaseComponent[]{ new TextComponent(json) }));

                            // Send the custom message to everyone on the server
                            for (Player online : getServer().getOnlinePlayers()) {
                                online.spigot().sendMessage(textComponent);
                            }

                            cooldowns.put(player.getUniqueId(), System.currentTimeMillis());

                        } else {
                            player.sendMessage(ChatColor.RED + "You can only execute this command every 60 seconds.");
                        }

                    } else {
                        player.sendMessage(ChatColor.RED + "You must be holding an item to execute this command.");
                    }

                } else {
                    player.sendMessage(ChatColor.RED + "You do not have permission to execute this command.");
                }

            } else {
                sender.sendMessage(ChatColor.RED + "Only players can execute this command.");
            }

            return true;

        });

    }

    /* Convert ItemStack to a valid display name */
    private String getItemName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            String displayName = item.getItemMeta().getDisplayName();
            if (ChatColor.stripColor(displayName).equals(displayName)) {
                displayName = ChatColor.AQUA.toString() + ChatColor.ITALIC + displayName;
            }
            return displayName;
        }

        StringBuilder displayName = new StringBuilder();
        for (String str : item.getType().toString().split("_")) {
            displayName.append(str.substring(0, 1).toUpperCase()).append(str.substring(1).toLowerCase()).append(" ");
        }

        return displayName.toString().trim();
    }

}
