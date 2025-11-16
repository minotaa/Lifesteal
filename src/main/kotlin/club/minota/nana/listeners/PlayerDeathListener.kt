package club.minota.nana.listeners

import club.minota.nana.Nana
import club.minota.nana.utils.Settings
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.inventory.ItemStack
import kotlin.math.floor

class PlayerDeathListener : Listener {
    fun inventoryFull(player: Player): Boolean {
        return player.inventory.firstEmpty() == -1
    }

    fun bulkItems(player: Player, bulk: ArrayList<ItemStack>) {
        for (item in bulk) {
            if (!inventoryFull(player)) {
                player.inventory.addItem(item)
            } else {
                player.world.dropItemNaturally(player.location, item)
            }
        }
    }

    @EventHandler
    fun onPlayerDeath(e: PlayerDeathEvent) {
        if (e.entity.killer is Player) {
            val killer = e.entity.killer as Player
            val maxHealth = killer.getAttribute(Attribute.MAX_HEALTH)!!.baseValue
            e.player.getAttribute(Attribute.MAX_HEALTH)!!.baseValue = e.player.getAttribute(
                Attribute.MAX_HEALTH)!!.value - 2.0
            e.player.sendMessage(MiniMessage.miniMessage().deserialize("<red>[❤]</red><white> ${e.entity.killer!!.name} has taken </white><color:#eb2626>1❤</color><white> from you! You have <color:#eb2626>${e.player.getAttribute(
                Attribute.MAX_HEALTH)!!.value.toInt() / 2}❤</color> remaining.<white>"))
            e.entity.killer!!.sendMessage(MiniMessage.miniMessage().deserialize("<red>[❤]</red><white> You took </white><color:#eb2626>1❤</color><white> from ${e.player.name}!"))

            if (e.player.getAttribute(Attribute.MAX_HEALTH)!!.value.toInt() <= 0) {
                Bukkit.broadcast(MiniMessage.miniMessage().deserialize("<red>${e.player.name} has lost all their hearts and is now permanently dead."))
                e.player.kick(MiniMessage.miniMessage().deserialize("You've lost all your hearts and are permanently dead."))
            }

            if (maxHealth >= (Settings.config.getDouble("options.max-lifesteal-hearts") * 2.0)) {
                if (Settings.config.getBoolean("options.heart-dropping")) {
                    val heartItem = ItemStack(Material.RED_DYE)
                    val heartItemMeta = heartItem.itemMeta
                    heartItemMeta.displayName(MiniMessage.miniMessage().deserialize("<color:#eb2626>Heart"))
                    heartItemMeta.lore(listOf(
                        MiniMessage.miniMessage().deserialize("<gray>Right-click this item to add <color:#eb2626>1❤</color> to your health bar.</gray>")
                    ))
                    heartItemMeta.itemModel = NamespacedKey("aprilsteal", "heart")
                    heartItem.itemMeta = heartItemMeta
                    e.player.world.dropItemNaturally(e.player.location, heartItem)
                    e.entity.killer!!.sendMessage(MiniMessage.miniMessage().deserialize("<red>[❤]</red><white> Your health bar is full! The heart has dropped as an item."))
                    bulkItems(killer, arrayListOf(heartItem))
                }
            } else {
                if (Settings.config.getBoolean("options.absorbing")) {
                    e.entity.killer!!.sendMessage(MiniMessage.miniMessage().deserialize("<red>[❤]</red><white> You absorbed the heart! You now have <color:#eb2626>${floor(maxHealth + 2.0).toInt() / 2}❤</color>."))
                    killer.getAttribute(Attribute.MAX_HEALTH)!!.baseValue = maxHealth + 2.0
                }
            }
        } else {
            e.player.sendMessage(MiniMessage.miniMessage().deserialize("<red>[❤]</red> You've died and lost <color:#eb2626>1❤</color>! The heart has dropped as an item for anyone to pick up."))
            e.player.getAttribute(Attribute.MAX_HEALTH)!!.baseValue = e.player.getAttribute(
                Attribute.MAX_HEALTH)!!.value - 2.0

            if (Settings.config.getBoolean("options.heart-dropping")) {
                val heartItem = ItemStack(Material.RED_DYE)
                val heartItemMeta = heartItem.itemMeta
                heartItemMeta.displayName(MiniMessage.miniMessage().deserialize("<color:#eb2626>Heart"))
                heartItemMeta.lore(listOf(
                    MiniMessage.miniMessage().deserialize("<gray>Right-click this item to add <color:#eb2626>1❤</color> to your health bar.</gray>")
                ))
                heartItemMeta.itemModel = NamespacedKey("aprilsteal", "heart")
                heartItem.itemMeta = heartItemMeta
                e.player.world.dropItemNaturally(e.player.location, heartItem)
            }
        }

        Nana.inst.postToActivityLog("**${e.entity.name}** died & lost a heart! They now have ${floor(e.player.getAttribute(Attribute.MAX_HEALTH)!!.value / 2).toInt()} hearts!")
    }

    @EventHandler
    fun onPlayerRespawn(e: PlayerRespawnEvent) {
        if ((e.player.getAttribute(Attribute.MAX_HEALTH)!!.value.toInt() / 2) == 0) {
            if (!Settings.config.getBoolean("options.ban-on-death")) {
                e.player.gameMode = GameMode.SPECTATOR
            } else {
                if (e.player.isOp){
                    e.player.kick(MiniMessage.miniMessage().deserialize("You've lost all your hearts and are permanently dead."))
                }
            }
            Nana.inst.postToActivityLog("**${e.player.name}** lost all their hearts! They're eliminated!")
            e.player.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>You've been set to Spectator mode as you've lost all your hearts."))
        }
    }
}