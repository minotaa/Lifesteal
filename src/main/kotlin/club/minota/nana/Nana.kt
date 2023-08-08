package club.minota.nana

import club.minota.nana.commands.ToggleEndCommand
import club.minota.nana.commands.ToggleNetherCommand
import club.minota.nana.listeners.*
import club.minota.nana.utils.Settings
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.Scoreboard
import kotlin.math.floor

class Nana : JavaPlugin() {
    companion object {
        lateinit var inst: Nana
    }

    override fun onEnable() {
        inst = this
        Settings

        if (Settings.data!!.getBoolean("nether") == null) {
            Settings.data.set("nether", false)
        }
        if (Settings.data!!.getBoolean("end") == null) {
            Settings.data.set("end", false)
        }
        Settings.save()

        val heartItem = ItemStack(Material.RED_DYE)
        val heartItemMeta = heartItem.itemMeta
        heartItemMeta.displayName(MiniMessage.miniMessage().deserialize("<color:#eb2626>Heart Item"))
        heartItemMeta.lore(listOf(
            MiniMessage.miniMessage().deserialize( "<gray>This item, once right clicked, will grant you an extra heart!</gray>")
        ))
        heartItem.itemMeta = heartItemMeta
        val key = NamespacedKey(this, "heart_item")
        val recipe = ShapedRecipe(key, heartItem)
        recipe.shape("NTN", "TGT", "NTN")
        recipe.setIngredient('N', Material.NETHERITE_INGOT)
        recipe.setIngredient('T', Material.TOTEM_OF_UNDYING)
        recipe.setIngredient('G', Material.GOLDEN_APPLE)
        Bukkit.addRecipe(recipe)

        Bukkit.getServer().pluginManager.registerEvents(PlayerDeathListener(), this)
        Bukkit.getServer().pluginManager.registerEvents(PlayerJoinListener(), this)
        Bukkit.getServer().pluginManager.registerEvents(PlayerInteractListener(), this)
        Bukkit.getServer().pluginManager.registerEvents(PlayerChatListener(), this)
        Bukkit.getServer().pluginManager.registerEvents(PortalListener(), this)

        this.getCommand("toggleend")!!.setExecutor(ToggleEndCommand())
        this.getCommand("togglenether")!!.setExecutor(ToggleNetherCommand())

        Bukkit.getLogger().info("The plugin has successfully loaded.")

        val manager = Bukkit.getScoreboardManager()
        val board: Scoreboard = manager.mainScoreboard
        val name: Objective = if (board.getObjective("HealthNamePL") == null) {
            board.registerNewObjective("HealthNamePL", "dummy")
        } else {
            board.getObjective("HealthNamePL")!!
        }
        val tab: Objective = if (board.getObjective("HealthTabPL") == null) {
            board.registerNewObjective("HealthTabPL", "dummy")
        } else {
            board.getObjective("HealthTabPL")!!
        }
        name.displaySlot = DisplaySlot.BELOW_NAME
        name.displayName(MiniMessage.miniMessage().deserialize("<color:#eb2626>❤</color>"))
        tab.displaySlot = DisplaySlot.PLAYER_LIST
        Bukkit.getScheduler().runTaskTimer(this, Runnable {
            for (player in Bukkit.getOnlinePlayers()) {
                val health = floor(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.value / 2).toInt()
                name.getScore(player.name).score = health
                tab.getScore(player.name).score = health
            }
        }, 1L, 1L)
    }

    override fun onDisable() {
        Bukkit.getLogger().info("The plugin has successfully unloaded.")
    }
}