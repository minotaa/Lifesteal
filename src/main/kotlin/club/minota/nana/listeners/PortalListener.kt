package club.minota.nana.listeners

import club.minota.nana.utils.Settings
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerPortalEvent
import org.bukkit.event.player.PlayerTeleportEvent

class PortalListener : Listener {
    @EventHandler
    fun onPlayerPortal(e: PlayerPortalEvent) {
        if (e.cause == PlayerTeleportEvent.TeleportCause.END_PORTAL) {
            if (Settings.data!!.getBoolean("end") == false) {
                e.isCancelled = true
                e.player.sendMessage(MiniMessage.miniMessage().deserialize("<red>The End is currently disabled right now, try again later!</red>"))
                return
            }
        }
        if (e.cause == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) {
            if (Settings.data!!.getBoolean("nether") == false) {
                e.isCancelled = true
                e.player.sendMessage(MiniMessage.miniMessage().deserialize("<red>The Nether is currently disabled right now, try again later!</red>"))
                return
            }
        }
    }
}