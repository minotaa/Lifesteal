package club.minota.nana

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

class Nana : JavaPlugin() {
    companion object {
        lateinit var inst: Nana
    }

    override fun onEnable() {
        inst = this
        Bukkit.getLogger().info("The plugin has successfully loaded.")
    }

    override fun onDisable() {
        Bukkit.getLogger().info("The plugin has successfully unloaded.")
    }
}