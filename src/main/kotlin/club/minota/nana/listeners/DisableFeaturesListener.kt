package club.minota.nana.listeners

import club.minota.nana.utils.Settings
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack

class DisableFeaturesListener : Listener {
    @EventHandler
    fun onInventoryOpen(e: InventoryOpenEvent) {
        if (e.inventory.type == InventoryType.ENCHANTING && !Settings.config.getBoolean("options.enchanting")) {
            e.isCancelled = true
            e.player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Enchanting is disabled!"))
        }
        if ((e.inventory.type == InventoryType.SMITHING) && !Settings.config.getBoolean("options.smithing")) {
            e.isCancelled = true
            e.player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Smithing is disabled!"))
        }
    }

    @EventHandler
    fun onPlayerInteract(e: PlayerInteractEvent) {
        val block = e.clickedBlock ?: return
        when (block.type) {
            Material.ENCHANTING_TABLE -> {
                if (!Settings.config.getBoolean("options.enchanting")) {
                    e.isCancelled = true
                    e.player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Enchanting is disabled!"))
                }
            }
            Material.SMITHING_TABLE -> {
                if (!Settings.config.getBoolean("options.smithing")) {
                    e.isCancelled = true
                    e.player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Smithing is disabled!"))
                }
            }
            else -> {
                return
            }
        }
    }

    @EventHandler
    fun onPrepareItemCraftEvent(e: PrepareItemCraftEvent) {
        val result = e.inventory.result ?: return
        if (Settings.config.getBoolean("options.netherite-ingot-crafting")) return
        if (result.type == Material.NETHERITE_INGOT) {
            e.inventory.result = null
            e.viewers.forEach { viewer ->
                viewer.sendMessage(MiniMessage.miniMessage().deserialize("<red>Crafting Netherite Ingots is disabled!"))
            }
        }
    }

    @EventHandler
    fun onAnvilPrepare(event: PrepareAnvilEvent) {
        val firstItem = event.inventory.firstItem ?: return
        val secondItem = event.inventory.secondItem ?: return
        val result = event.result ?: return
        if (Settings.config.getBoolean("options.enchanting")) return

        if (secondItem.type.name.contains("ENCHANTED_BOOK")) {
            event.result = null
            event.viewers.forEach { viewer ->
                viewer.sendMessage(MiniMessage.miniMessage().deserialize("<red>Applying enchantments is disabled!"))
            }
            return
        }

        val firstEnchants = firstItem.enchantments
        val resultEnchants = result.enchantments

        if (resultEnchants.size > firstEnchants.size ||
            resultEnchants.keys.any { it !in firstEnchants.keys }) {
            event.result = null
            event.viewers.forEach { viewer ->
                viewer.sendMessage(MiniMessage.miniMessage().deserialize("<red>Applying enchantments is disabled!"))
            }
        }
    }
}