package club.minota.nana.listeners

import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.GameMode
import org.bukkit.attribute.Attribute
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

class PlayerJoinListener : Listener {
    @EventHandler
    fun onPlayerJoin(e: PlayerJoinEvent) {
        if ((e.player.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.value.toInt() / 2) == 0) {
            e.player.gameMode = GameMode.SPECTATOR
            e.player.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>You've been set to Spectator mode as you've lost all your hearts."))
        }
    }
}