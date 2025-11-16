package club.minota.nana.listeners

import club.minota.nana.Nana
import club.minota.nana.utils.Settings
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.ItemSpawnEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import kotlin.math.floor

class PlayerInteractListener : Listener {
    fun similarItems(one: ItemStack?, two: ItemStack?): Boolean {
        if (one == null || two == null) {
            return one === two
        }
        if (one.isSimilar(two)) {
            return true
        }
        if (one.type != two.type || one.durability != two.durability || one.hasItemMeta() && two.hasItemMeta() && one.itemMeta.javaClass != two.itemMeta.javaClass) {
            return false
        }
        if (!one.hasItemMeta() && !two.hasItemMeta()) {
            return true
        }
        val oneMeta = one.itemMeta
        val twoMeta = two.itemMeta
        if (oneMeta === twoMeta || oneMeta == null || twoMeta == null) {
            return oneMeta === twoMeta
        }
        val oneSerMeta = oneMeta.serialize()
        val twoSerMeta = twoMeta.serialize()
        if (oneSerMeta == twoSerMeta) {
            return true
        }
        return false
    }

    private fun effectiveSize(inventory: Inventory): Int {
        return getStorageContents(inventory).size
    }

    private var legacyContents: Boolean? = null
    private fun getStorageContents(inventory: Inventory): Array<ItemStack?> {
        if (legacyContents == null) {
            try {
                inventory.storageContents
                legacyContents = false
            } catch (e: NoSuchMethodError) {
                legacyContents = true
            }
        }
        return if (legacyContents == true) inventory.contents else inventory.storageContents
    }


    fun removeOne(item: ItemStack, inventory: Inventory): Int {
        var amountLeft = 1
        var currentSlot = 0
        while (currentSlot < effectiveSize(inventory) && amountLeft > 0) {
            val currentItem = inventory.getItem(currentSlot)
            if (currentItem != null && similarItems(currentItem, item)) {
                val neededToRemove = Math.min(currentItem.amount, amountLeft)
                currentItem.amount = currentItem.amount - neededToRemove
                inventory.setItem(currentSlot, currentItem)
                amountLeft -= neededToRemove
            }
            currentSlot++
        }
        return amountLeft
    }

    @EventHandler
    fun onItemSpawn(event: ItemSpawnEvent) {
        val item = event.entity
        val itemStack = item.itemStack

        if (itemStack.type != Material.RED_DYE && itemStack.type != Material.SKULL_BANNER_PATTERN) return

        val itemMeta = itemStack.itemMeta ?: return
        val itemModel = itemMeta.itemModel ?: return

        if (itemModel == NamespacedKey("aprilsteal", "heart")) {
            item.isUnlimitedLifetime = true
            item.isInvulnerable = true
        } else if (itemModel == NamespacedKey("aprilsteal", "key")) {
            item.isInvulnerable = true
            item.isUnlimitedLifetime = true
        }
    }

    @EventHandler
    fun onPlayerInteract(e: PlayerInteractEvent) {
        val item = e.item ?: return
        if (!item.hasItemMeta()) return
        if (e.action != Action.RIGHT_CLICK_AIR && e.action != Action.RIGHT_CLICK_BLOCK) return

        val itemMeta = item.itemMeta
        val itemModel = itemMeta.itemModel ?: return

        if (itemModel == NamespacedKey("aprilsteal", "heart")) {
            e.isCancelled = true
            val maxHealth = e.player.getAttribute(Attribute.MAX_HEALTH)!!.baseValue
            if (maxHealth >= (Settings.config.getDouble("options.max-lifesteal-hearts") * 2.0)) {
                e.player.sendMessage(MiniMessage.miniMessage().deserialize("<red>[❤]</red> You are at the maximum amount of extra health."))
                return
            }

            removeOne(e.item!!, e.player.inventory)
            e.player.getAttribute(Attribute.MAX_HEALTH)!!.baseValue = maxHealth + 2.0
            e.player.sendMessage(MiniMessage.miniMessage().deserialize("<red>[❤]</red> You have redeemed <color:#eb2626>1❤</color> and now have <color:#eb2626>${e.player.getAttribute(
                Attribute.MAX_HEALTH)!!.value.toInt() / 2}❤</color><white>."))
            Nana.inst.postToActivityLog("**${e.player.name}** redeemed a heart! They now have ${floor(e.player.getAttribute(Attribute.MAX_HEALTH)!!.value / 2).toInt()} hearts now!")
        }
    }
}