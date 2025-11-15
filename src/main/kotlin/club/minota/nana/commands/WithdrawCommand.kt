package club.minota.nana.commands

import club.minota.nana.Nana
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import kotlin.math.floor

class WithdrawCommand : CommandExecutor {
    fun inventoryFull(player: Player): Boolean {
        return player.inventory.firstEmpty() == -1
    }

    fun bulkItems(player: Player, bulk: ArrayList<ItemStack>) {
        for (item in bulk) {
            if (!inventoryFull(player)) {
                player.inventory.addItem(item)
            } else {
                player.world.dropItemNaturally(player.location, item)
            }
        }
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("You can't use this command as you are not a player.")
            return false
        }
        val mh = floor(sender.getAttribute(Attribute.MAX_HEALTH)!!.value / 2).toInt()
        if (mh < 2) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>You don't have enough hearts to use this command!</red>"))
            return false
        }
        sender.getAttribute(Attribute.MAX_HEALTH)!!.baseValue = sender.getAttribute(
            Attribute.MAX_HEALTH)!!.value - 2.0
        val heartItem = ItemStack(Material.RED_DYE)
        val heartItemMeta = heartItem.itemMeta
        heartItemMeta.displayName(MiniMessage.miniMessage().deserialize("<color:#eb2626>Heart"))
        heartItemMeta.lore(listOf(
            MiniMessage.miniMessage().deserialize("<gray>Right-click this item to add <color:#eb2626>1❤</color> to your health bar.</gray>")
        ))
        heartItemMeta.itemModel = NamespacedKey("aprilsteal", "heart")
        heartItem.itemMeta = heartItemMeta
        bulkItems(sender, arrayListOf(heartItem))
        Nana.inst.postToActivityLog("**${sender.name}** withdrew a heart! They now have ${floor(sender.getAttribute(Attribute.MAX_HEALTH)!!.value / 2).toInt()} hearts now!")
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>Successfully withdrew <color:#eb2626>1❤</color>! If your inventory is full, it has been dropped on the floor."))
        return true
    }
}