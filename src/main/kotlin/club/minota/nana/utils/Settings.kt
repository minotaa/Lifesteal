package club.minota.nana.utils

import club.minota.nana.Nana
import org.bukkit.Bukkit
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.IOException

class Settings private constructor() {
    companion object {
        var data: FileConfiguration
        var f: File
        init {
            val plugin = Nana.inst
            if (!plugin.dataFolder.exists()) {
                plugin.dataFolder.mkdir()
            }
            f = File(plugin.dataFolder, "config.yml")
            if (!f.exists()) {
                try {
                    f.createNewFile()
                    Bukkit.getLogger().info("Successfully created new config file.")
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            data = YamlConfiguration.loadConfiguration(f)
        }

        fun save() {
            try {
                data.save(f)
                Bukkit.getLogger().info("Saved config.yml")
            } catch (ex: IOException) {
                ex.printStackTrace()
            }
        }

        fun reload() {
            data = YamlConfiguration.loadConfiguration(f)
            Bukkit.getLogger().info("Reloaded config.yml")
        }
    }
}