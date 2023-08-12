package club.minota.nana.commands

import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
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

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {
        if (sender !is Player) {
            sender.sendMessage("You can't use this command as you are not a player.")
            return false
        }
        val mh = floor(sender.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.value / 2).toInt()
        if (mh < 2) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>You have too little hearts to use this command!</red>"))
            return false
        }
        sender.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.baseValue = sender.getAttribute(
            Attribute.GENERIC_MAX_HEALTH)!!.value - 2.0
        val heartItem = ItemStack(Material.RED_DYE)
        val heartItemMeta = heartItem.itemMeta
        heartItemMeta.displayName(MiniMessage.miniMessage().deserialize("<color:#eb2626>Heart Item"))
        heartItemMeta.lore(listOf(
            MiniMessage.miniMessage().deserialize("<gray>This item, once right clicked, will grant you an extra heart!</gray>")
        ))
        heartItem.itemMeta = heartItemMeta
        bulkItems(sender, arrayListOf(heartItem))
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>Successfully granted you a Heart Item! If your inventory is full, it might've been dropped on the floor!"))
        return true
    }
}