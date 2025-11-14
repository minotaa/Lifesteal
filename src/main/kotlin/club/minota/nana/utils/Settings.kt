package club.minota.nana.utils

import club.minota.nana.Nana
import org.bukkit.Bukkit
import org.bukkit.configuration.InvalidConfigurationException
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.IOException

class Settings private constructor() {
    companion object {
        var data: FileConfiguration
        var config: FileConfiguration

        var df: File
        var cf: File
        init {
            val plugin = Nana.inst
            if (!plugin.dataFolder.exists()) {
                plugin.dataFolder.mkdir()
            }
            df = File(plugin.dataFolder, "data.yml")
            if (!df.exists()) {
                try {
                    df.createNewFile()
                    Bukkit.getLogger().info("Successfully created new data file.")
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            data = YamlConfiguration.loadConfiguration(df)

            cf = File(Nana.inst.dataFolder, "config.yml")
            if (!(cf.exists())) {
                cf.parentFile.mkdir()
                Nana.inst.saveResource("config.yml", false)
            }
            try {
                config = YamlConfiguration.loadConfiguration(cf)
            } catch (e: IOException) {
                throw RuntimeException(e)
            } catch (e: InvalidConfigurationException) {
                throw RuntimeException(e)
            }
        }

        fun save() {
            try {
                data.save(df)
                Bukkit.getLogger().info("Saved data.yml")
                config.save(cf)
                Bukkit.getLogger().info("Saved config.yml")
            } catch (ex: IOException) {
                ex.printStackTrace()
            }
        }

        fun reload() {
            data = YamlConfiguration.loadConfiguration(df)
            Bukkit.getLogger().info("Reloaded data.yml")
            config = YamlConfiguration.loadConfiguration(cf)
            Bukkit.getLogger().info("Reloaded config.yml")
        }
    }
}