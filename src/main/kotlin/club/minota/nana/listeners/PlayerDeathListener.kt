package club.minota.nana.listeners

import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.inventory.ItemStack

class PlayerDeathListener : Listener {
    @EventHandler
    fun onPlayerDeath(e: PlayerDeathEvent) {
        if (e.entity.killer is Player) {
            val heartItem = ItemStack(Material.RED_DYE)
            val heartItemMeta = heartItem.itemMeta
            heartItemMeta.displayName(MiniMessage.miniMessage().deserialize("<color:#eb2626>Heart Item"))
            heartItemMeta.lore(listOf(
                MiniMessage.miniMessage().deserialize("<gray>This item, once right clicked, will grant you an extra heart!</gray>")
            ))
            heartItem.itemMeta = heartItemMeta
            e.drops.add(heartItem)
            e.player.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.baseValue = e.player.getAttribute(
                Attribute.GENERIC_MAX_HEALTH)!!.value - 2.0
            e.player.sendMessage(MiniMessage.miniMessage().deserialize("<red>!!!</red><white> ${e.entity.killer!!.name} took </white><color:#eb2626>1❤</color><white> from you! (remaining hearts: <color:#eb2626>${e.player.getAttribute(
                Attribute.GENERIC_MAX_HEALTH)!!.value.toInt() / 2}❤</color><white>) <red>!!!</red>"))
            e.entity.killer!!.sendMessage(MiniMessage.miniMessage().deserialize("<red>!!!</red><white> You took </white><color:#eb2626>1❤</color><white> from ${e.player.name}! <red>!!!</red>"))
        }
    }

    @EventHandler
    fun onPlayerRespawn(e: PlayerRespawnEvent) {
        if ((e.player.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.value.toInt() / 2) == 0) {
            e.player.gameMode = GameMode.SPECTATOR
            Bukkit.broadcast(MiniMessage.miniMessage().deserialize("<red>${e.player.name} lost all their hearts! They're eliminated!"))
            e.player.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>You've been set to Spectator mode as you've lost all your hearts."))
        }
    }
}