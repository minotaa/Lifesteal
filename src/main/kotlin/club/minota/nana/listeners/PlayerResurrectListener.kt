package club.minota.nana.listeners

import club.minota.nana.utils.Settings
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityResurrectEvent

class PlayerResurrectListener : Listener {
    @EventHandler
    fun onPlayerResurrect(e: EntityResurrectEvent) {
        if (!Settings.config.getBoolean("options.resurrecting")) {
            e.isCancelled = true
        }
    }
}