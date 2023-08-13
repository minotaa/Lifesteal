package club.minota.nana.listeners

import club.minota.nana.Nana
import net.kyori.adventure.text.TextComponent
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerAdvancementDoneEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class ActivityLogListener : Listener {
    @EventHandler
    fun onPlayerAdvancement(e: PlayerAdvancementDoneEvent) {
        Nana.inst.postToActivityLog("**${e.player.name}** has completed the advancement [${(e.advancement.displayName() as TextComponent).content()}]")
    }

    @EventHandler
    fun onPlayerJoin(e: PlayerJoinEvent) {
        Nana.inst.postToActivityLog("**${e.player.name}** has joined the server! (${Bukkit.getOnlinePlayers().size} online)")
    }

    @EventHandler
    fun onPlayerQuit(e: PlayerQuitEvent) {
        Nana.inst.postToActivityLog("**${e.player.name}** has left the server! (${Bukkit.getOnlinePlayers().size} online)")
    }

    @EventHandler
    fun onPlayerDeath(e: PlayerDeathEvent) {
        Nana.inst.postToActivityLog((e.deathMessage() as TextComponent).content())
    }
}