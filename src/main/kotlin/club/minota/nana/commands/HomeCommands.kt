package club.minota.nana.commands

import club.minota.nana.listeners.CombatTagListener
import club.minota.nana.utils.Settings
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class SetHomeCommand : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {
        if (sender !is Player) {
            sender.sendMessage("You can't use this command as you aren't a player!")
            return false
        }
        Settings.data!!.set("homes.${sender.uniqueId}.x", sender.location.x)
        Settings.data!!.set("homes.${sender.uniqueId}.y", sender.location.y)
        Settings.data!!.set("homes.${sender.uniqueId}.z", sender.location.z)
        Settings.data!!.set("homes.${sender.uniqueId}.world", sender.location.world.name)
        Settings.save()
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Successfully set your home to your location!"))
        return true
    }
}

class HomeCommand : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {
        if (sender !is Player) {
            sender.sendMessage("You can't use this command as you are not a player!")
            return false
        }
        if (CombatTagListener.tags[sender.uniqueId] != null) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>You currently have a pending combat tag, you may not use this command yet."))
            return false
        }
        if (Settings.data!!.getDouble("homes.${sender.uniqueId}.x") == null) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>You don't seem to have a home set, set one using /sethome."))
            return false
        }
        val location = Location(
            Bukkit.getWorld(Settings.data!!.getString("homes.${sender.uniqueId}.world")!!),
            Settings.data!!.getDouble("homes.${sender.uniqueId}.x"),
            Settings.data!!.getDouble("homes.${sender.uniqueId}.y"),
            Settings.data!!.getDouble("homes.${sender.uniqueId}.z")
        )
        sender.teleportAsync(location)
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Teleported you to your home!"))
        return true
    }
}