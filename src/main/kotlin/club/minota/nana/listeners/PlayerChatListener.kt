package club.minota.nana.listeners

import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.attribute.Attribute
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class PlayerChatListener : Listener {
    @EventHandler
    fun onPlayerChat(e: AsyncChatEvent) {
        e.isCancelled = true
        Bukkit.broadcast(MiniMessage.miniMessage().deserialize("<color:#eb2626>${e.player.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.value.toInt() / 2}❤</color> <reset>${e.player.name}: ${(e.message() as TextComponent).content()}</reset>"))
    }
}