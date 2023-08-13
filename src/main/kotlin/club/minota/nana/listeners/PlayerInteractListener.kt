package club.minota.nana.listeners

import club.minota.nana.Nana
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.attribute.Attribute
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
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
    fun onPlayerInteract(e: PlayerInteractEvent) {
        if (e.hasItem() && e.item!!.hasItemMeta() && e.item!!.itemMeta.displayName() == MiniMessage.miniMessage().deserialize("<color:#eb2626>Heart Item")) {
            e.isCancelled = true
            removeOne(e.item!!, e.player.inventory)
            e.player.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.baseValue = e.player.getAttribute(
                Attribute.GENERIC_MAX_HEALTH)!!.value + 2.0
            e.player.sendMessage(MiniMessage.miniMessage().deserialize("<red>!!!</red> You have redeemed a heart item! (new hearts: <color:#eb2626>${e.player.getAttribute(
                Attribute.GENERIC_MAX_HEALTH)!!.value.toInt() / 2}❤</color><white>) <red>!!!</red>"))
            Nana.inst.postToActivityLog("**${e.player.name}** redeemed a heart! They now have ${floor(e.player.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.value / 2).toInt()} hearts now!")
        }
    }
}