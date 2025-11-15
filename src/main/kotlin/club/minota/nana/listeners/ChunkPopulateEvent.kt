package club.minota.nana.listeners

import club.minota.nana.Nana
import club.minota.nana.events.ChunkModifiableEvent
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkPopulateEvent
import org.bukkit.scheduler.BukkitRunnable

class ChunkPopulateListener : Listener {

    @EventHandler
    fun on(event: ChunkPopulateEvent) {
        val worldName = event.world.name

        val chunkX = event.chunk.x
        val chunkZ = event.chunk.z

        object : BukkitRunnable() {
            override fun run() {
                val world = Bukkit.getWorld(worldName) ?: return
                val chunk = world.getChunkAt(chunkX, chunkZ)
                Bukkit.getPluginManager().callEvent(ChunkModifiableEvent(chunk))
            }
        }.runTaskLater(Nana.inst, 400L)
    }
}