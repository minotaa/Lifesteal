package club.minota.nana.listeners

import club.minota.nana.utils.Settings
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.bukkit.inventory.PlayerInventory

class PlayerTotemListener : Listener {
    @EventHandler
    fun onInventoryClick(e: InventoryClickEvent) {
        val player = e.whoClicked as Player
        val clickedInventory = e.clickedInventory as PlayerInventory
        if (e.slot == 40 && e.cursor.type == Material.TOTEM_OF_UNDYING) {
            if (!Settings.config.getBoolean("options.resurrecting")) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>!!!</red> Resurrection is currently disabled on this server — the Totem of Undying in your offhand will do nothing! <red>!!!</red>"))
            }
        }
    }

    @EventHandler
    fun onPlayerSwapHands(e: PlayerSwapHandItemsEvent) {
        val offHandItem = e.offHandItem
        if (offHandItem?.type == Material.TOTEM_OF_UNDYING) {
            if (!Settings.config.getBoolean("options.resurrecting")) {
                e.player.sendMessage(MiniMessage.miniMessage().deserialize("<red>!!!</red> Resurrection is currently disabled on this server — the Totem of Undying in your offhand will do nothing! <red>!!!</red>"))
            }
        }
    }
}