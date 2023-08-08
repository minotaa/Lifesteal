package club.minota.nana.commands

import club.minota.nana.utils.Settings
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class ToggleNetherCommand : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {
        if (sender is Player) {
            if (!sender.isOp) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Only operators can use this command.</red>"))
                return false
            }
        }
        if (Settings.data!!.getBoolean("nether") == true) {
            Bukkit.broadcast(MiniMessage.miniMessage().deserialize("<yellow>The Nether has been disabled!</yellow>"))
            Settings.data!!.set("nether", false)
            Settings.save()
        } else {
            Bukkit.broadcast(MiniMessage.miniMessage().deserialize("<yellow>The Nether has been enabled!</yellow>"))
            Settings.data!!.set("nether", true)
            Settings.save()
        }
        return true
    }
}