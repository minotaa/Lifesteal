package club.minota.nana.listeners

import club.minota.nana.utils.Settings
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.GameMode
import org.bukkit.attribute.Attribute
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerLoginEvent

class PlayerJoinListener : Listener {
    @EventHandler
    fun onPlayerJoin(e: PlayerJoinEvent) {
        if ((e.player.getAttribute(Attribute.MAX_HEALTH)!!.value.toInt() / 2) <= 0) {
            if (Settings.config.getBoolean("options.ban-on-death")) {
                e.player.kick(MiniMessage.miniMessage().deserialize("You've lost all your hearts and are permanently dead."))
            } else {
                e.player.gameMode = GameMode.SPECTATOR
                e.player.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>You've been set to Spectator mode as you've lost all your hearts."))
            }
        }
    }
}