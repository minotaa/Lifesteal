package club.minota.nana.listeners

import club.minota.nana.events.ChunkModifiableEvent
import club.minota.nana.utils.Settings
import com.google.common.collect.Lists
import com.google.common.collect.Sets
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import java.util.LinkedList
import java.util.Random

class OreLimiterListener : Listener {
    private var random = Random()
    val ores = listOf(
        Material.COAL_ORE,
        Material.IRON_ORE,
        Material.GOLD_ORE,
        Material.REDSTONE_ORE,
        Material.LAPIS_ORE,
        Material.DIAMOND_ORE,
        Material.EMERALD_ORE,
        Material.DEEPSLATE_COAL_ORE,
        Material.DEEPSLATE_IRON_ORE,
        Material.DEEPSLATE_GOLD_ORE,
        Material.DEEPSLATE_REDSTONE_ORE,
        Material.DEEPSLATE_LAPIS_ORE,
        Material.DEEPSLATE_DIAMOND_ORE,
        Material.DEEPSLATE_EMERALD_ORE,
        Material.COPPER_ORE,
        Material.DEEPSLATE_COPPER_ORE
    )

    fun getNearby(block: Block): List<Block> {
        val nearby: ArrayList<Block> = arrayListOf()

        for (dx in -1..1) {
            for (dy in -1..1) {
                for (dz in -1..1) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue
                    }
                    nearby.add(block.getRelative(dx, dy, dz))
                }
            }
        }
        return nearby
    }

    fun getVein(start: Block, maxVeinSize: Int): List<Block> {
        val toCheck: LinkedList<Block> = Lists.newLinkedList()
        val vein: ArrayList<Block> = Lists.newArrayList()

        toCheck.add(start)
        vein.add(start)

        while (!toCheck.isEmpty()) {
            val check = toCheck.poll()

            for (nearbyBlock in getNearby(check)) {
                if (vein.contains(nearbyBlock)) continue
                var type = nearbyBlock.type
                if (type != start.type) continue

                toCheck.add(nearbyBlock)
                vein.add(nearbyBlock)

                if (vein.size > maxVeinSize) {
                    return vein
                }
            }
        }
        return vein
    }

    fun getVein(start: Block): List<Block> {
        start.type
        return getVein(start, 100)
    }

    fun getBlocks(start: Block, radius: Int): ArrayList<Block> {
        val blocks = ArrayList<Block>()
        var x = start.location.x - radius
        while (x <= start.location.x + radius) {
            var y = start.location.y - radius
            while (y <= start.location.y + radius) {
                var z = start.location.z - radius
                while (z <= start.location.z + radius) {
                    val loc: Location = Location(start.world, x, y, z)
                    blocks.add(loc.block)
                    z++
                }
                y++
            }
            x++
        }
        return blocks
    }

    fun isVeinExposed(vein: List<Block>): Boolean {
        vein.forEach {
            getBlocks(it, 2).forEach { block ->
                if (block.type == Material.AIR ||
                    block.type == Material.WATER ||
                    block.type == Material.LAVA ||
                    block.type == Material.CAVE_AIR
                ) return true
            }
        }
        return false
    }

    fun getReplacementMaterial(ore: Material): Material {
        return when (ore) {
            Material.DEEPSLATE_COAL_ORE,
            Material.DEEPSLATE_IRON_ORE,
            Material.DEEPSLATE_GOLD_ORE,
            Material.DEEPSLATE_REDSTONE_ORE,
            Material.DEEPSLATE_LAPIS_ORE,
            Material.DEEPSLATE_DIAMOND_ORE,
            Material.DEEPSLATE_EMERALD_ORE,
            Material.DEEPSLATE_COPPER_ORE -> Material.DEEPSLATE
            else -> Material.STONE
        }
    }

    @EventHandler
    fun on(event: ChunkModifiableEvent) {
        val chunk = event.chunk
        val checked: MutableSet<Block> = Sets.newHashSet()

        val goldRate = Settings.config.getInt("options.gold-removed")
        val diamondRate = Settings.config.getInt("options.diamonds-removed")

        for (x in 0..15) {
            for (y in 0..256) {
                for (z in 0..15) {
                    val block = chunk.getBlock(x, y, z)

                    if (checked.contains(block)) continue

                    val type = block.type

                    if (!ores.contains(type)) continue

                    val vein: List<Block> = getVein(block)
                    checked.addAll(vein)

                    val isExposed = isVeinExposed(vein)
                    val replacementMaterial = getReplacementMaterial(type)

                    val isGold = type == Material.GOLD_ORE || type == Material.DEEPSLATE_GOLD_ORE
                    val isDiamond = type == Material.DIAMOND_ORE || type == Material.DEEPSLATE_DIAMOND_ORE

                    if (isGold) {
                        val adjustedRate = if (!isExposed) (goldRate * 1.5).toInt() else goldRate
                        if ((random.nextInt(99) + 1) <= adjustedRate) {
                            vein.forEach { it.type = replacementMaterial }
                        }
                    } else if (isDiamond) {
                        val adjustedRate = if (!isExposed) (diamondRate * 1.5).toInt() else diamondRate
                        if ((random.nextInt(99) + 1) <= adjustedRate) {
                            vein.forEach { it.type = replacementMaterial }
                        }
                    }
                }
            }
        }
    }
}