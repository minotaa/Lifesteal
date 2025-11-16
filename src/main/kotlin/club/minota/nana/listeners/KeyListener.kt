package club.minota.nana.listeners

import club.minota.nana.Nana
import club.minota.nana.listeners.CombatTagListener
import club.minota.nana.utils.Settings
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.entity.ItemDespawnEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.event.block.Action
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitTask
import org.bukkit.persistence.PersistentDataType
import org.bukkit.NamespacedKey
import java.util.*

data class KeyData(
    val keyId: String,
    val deadPlayerUuid: String,
    val droppedTime: Long,
    val inventory: Map<Int, ItemStack>,
    val location: Location?
)

data class PreservedItems(
    val items: List<ItemStack>
)

class KeySystem : Listener {
    private val activeKeys = mutableMapOf<String, KeyData>()
    private val droppedKeyEntities = mutableMapOf<UUID, String>() // Item entity UUID -> Key ID
    private val voidCheckTasks = mutableMapOf<String, BukkitTask>()
    private val preservedItems = mutableMapOf<UUID, PreservedItems>() // Player UUID -> preserved items
    private val mm = MiniMessage.miniMessage()
    private val keyIdKey = NamespacedKey(Nana.inst, "key_id")

    init {
        loadPersistedData()
    }

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val deadPlayer = event.entity

        // Store the inventory directly from the player's inventory before it's cleared
        val inventory = mutableMapOf<Int, ItemStack>()
        val preserved = mutableListOf<ItemStack>()

        deadPlayer.inventory.contents.forEachIndexed { index, item ->
            if (item != null) {
                // Check if item should be preserved (armor, tools, weapons)
                if (shouldPreserveItem(item)) {
                    preserved.add(item.clone())
                } else {
                    inventory[index] = item.clone()
                }
            }
        }

        // Store preserved items
        if (preserved.isNotEmpty()) {
            preservedItems[deadPlayer.uniqueId] = PreservedItems(preserved)
            Settings.data.set("preserved_items.${deadPlayer.uniqueId}", preserved)
            saveConfig()
        }

        // Clear the drops so items don't drop normally
        event.drops.clear()

        // Create key data
        val keyId = UUID.randomUUID().toString()
        val keyData = KeyData(
            keyId = keyId,
            deadPlayerUuid = deadPlayer.uniqueId.toString(),
            droppedTime = System.currentTimeMillis(),
            inventory = inventory,
            location = deadPlayer.location.clone()
        )

        activeKeys[keyId] = keyData
        persistKeyData(keyId, keyData)

        // Drop the key item at death location
        val keyItem = createKeyItem(keyId, deadPlayer.name)
        val droppedItem = event.entity.world.dropItemNaturally(event.entity.location, keyItem)

        // Make key indestructible and set age
        droppedItem.isInvulnerable = true
        droppedItem.pickupDelay = 0

        // Track the dropped item entity
        droppedKeyEntities[droppedItem.uniqueId] = keyId

        // Send location to player
        val loc = deadPlayer.location
        deadPlayer.sendMessage(mm.deserialize(
            "<gray>[☠]</gray><white> Your key dropped at </white><yellow>X: ${loc.blockX}, Y: ${loc.blockY}, Z: ${loc.blockZ}</yellow>"
        ))

        // Start void check task
        startVoidCheck(keyId, droppedItem)
    }

    private fun startVoidCheck(keyId: String, droppedItem: Item) {
        val task = Bukkit.getScheduler().runTaskTimer(Nana.inst, Runnable {
            // Check if item fell into void (Y < -64 for most worlds)
            if (droppedItem.location.y < -64) {
                val keyData = activeKeys[keyId] ?: return@Runnable
                val deadPlayer = Bukkit.getPlayer(UUID.fromString(keyData.deadPlayerUuid))

                if (deadPlayer != null && deadPlayer.isOnline) {
                    // Give key directly to player
                    val keyItem = createKeyItem(keyId, deadPlayer.name)
                    deadPlayer.inventory.addItem(keyItem)
                    deadPlayer.sendMessage(mm.deserialize("<gray>[☠]</gray><white> Your key fell into the void and was returned to you!</white>"))
                }

                // Remove the dropped item
                droppedItem.remove()
                droppedKeyEntities.remove(droppedItem.uniqueId)

                // Cancel this task
                voidCheckTasks[keyId]?.cancel()
                voidCheckTasks.remove(keyId)
            }
        }, 10L, 10L) // Check every 0.5 seconds

        voidCheckTasks[keyId] = task
    }

    @EventHandler
    fun onItemDespawn(event: ItemDespawnEvent) {
        val itemEntity = event.entity
        val keyId = droppedKeyEntities[itemEntity.uniqueId] ?: return

        // Key should never despawn since it's indestructible, but just in case
        // cancel the void check
        droppedKeyEntities.remove(itemEntity.uniqueId)
        voidCheckTasks[keyId]?.cancel()
        voidCheckTasks.remove(keyId)
    }

    @EventHandler
    fun onEntityPickupItem(event: EntityPickupItemEvent) {
        val entity = event.entity
        if (entity !is Player) return

        val itemEntity = event.item
        val item = itemEntity.itemStack
        if (item.type != Material.SKULL_BANNER_PATTERN) return

        // Check if this is a key item
        val meta = item.itemMeta ?: return
        val keyId = meta.persistentDataContainer.get(keyIdKey, PersistentDataType.STRING) ?: return

        // Remove from tracked dropped entities and cancel void check
        droppedKeyEntities.remove(itemEntity.uniqueId)
        voidCheckTasks[keyId]?.cancel()
        voidCheckTasks.remove(keyId)

        // Check if this key exists
        val keyData = activeKeys[keyId] ?: return

        entity.sendMessage(mm.deserialize("<gray>[☠]</gray><white> You picked up a key from </white><yellow>${Bukkit.getOfflinePlayer(UUID.fromString(keyData.deadPlayerUuid)).name}</yellow><white>!</white>"))
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) return

        val item = event.item ?: return
        if (item.type != Material.SKULL_BANNER_PATTERN) return

        // Check if this is a key item
        val meta = item.itemMeta ?: return
        val keyId = meta.persistentDataContainer.get(keyIdKey, PersistentDataType.STRING) ?: return

        event.isCancelled = true

        // Check if player is combat logged
        if (CombatTagListener.tags[event.player.uniqueId] != null) {
            event.player.sendMessage(mm.deserialize("<gray>[☠]</gray><white> You cannot use a key while </white><red>combat logged</red><white>!</white>"))
            return
        }

        // Check if this key still exists
        val keyData = activeKeys[keyId]
        if (keyData == null) {
            event.player.sendMessage(mm.deserialize("<gray>[☠]</gray><white> This key is no longer valid!</white>"))
            item.amount = 0
            return
        }

        // Give player the inventory
        val items = ArrayList(keyData.inventory.values)
        bulkItems(event.player, items)

        // Remove key from system - it's been redeemed
        activeKeys.remove(keyId)
        removePersistedKeyData(keyId)

        // Remove the key item
        item.amount = 0

        event.player.sendMessage(mm.deserialize("<gray>[☠]</gray><white> You used the key and claimed the </white><green>items</green><white>!</white>"))
    }

    @EventHandler
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        val player = event.player

        // Give back preserved items
        preservedItems[player.uniqueId]?.let { preserved ->
            Bukkit.getScheduler().runTaskLater(Nana.inst, Runnable {
                preserved.items.forEach { item ->
                    if (inventoryFull(player)) {
                        player.world.dropItemNaturally(player.location, item)
                    } else {
                        player.inventory.addItem(item)
                    }
                }
                player.sendMessage(mm.deserialize("<gray>[☠]</gray><white> Your valuable items have been </white><green>preserved</green><white>!</white>"))
            }, 1L)

            preservedItems.remove(player.uniqueId)
            Settings.data.set("preserved_items.${player.uniqueId}", null)
            saveConfig()
        }
    }

    // Public function to get key location for commands
    fun getKeyLocation(playerUuid: UUID): Location? {
        // Find the most recent key for this player
        return activeKeys.values
            .filter { it.deadPlayerUuid == playerUuid.toString() }
            .maxByOrNull { it.droppedTime }
            ?.location
    }

    private fun shouldPreserveItem(item: ItemStack): Boolean {
        val type = item.type

        // Preserve armor
        if (type.name.endsWith("_HELMET") ||
            type.name.endsWith("_CHESTPLATE") ||
            type.name.endsWith("_LEGGINGS") ||
            type.name.endsWith("_BOOTS")) {
            return true
        }

        // Preserve tools
        if (type.name.endsWith("_PICKAXE") ||
            type.name.endsWith("_AXE") ||
            type.name.endsWith("_SHOVEL") ||
            type.name.endsWith("_HOE")) {
            return true
        }

        // Preserve weapons
        if (type.name.endsWith("_SWORD") ||
            type == Material.BOW ||
            type == Material.CROSSBOW ||
            type == Material.TRIDENT ||
            type == Material.MACE) {
            return true
        }

        // Preserve shields and elytra
        if (type == Material.SHIELD || type == Material.ELYTRA) {
            return true
        }

        return false
    }

    private fun createKeyItem(keyId: String, deadPlayerName: String): ItemStack {
        val item = ItemStack(Material.SKULL_BANNER_PATTERN)
        val meta = item.itemMeta
        meta?.displayName(mm.deserialize("<yellow>Key from $deadPlayerName</yellow>"))
        meta?.lore(listOf(
            mm.deserialize("<gray>Key ID: $keyId</gray>"),
            mm.deserialize("<gray>Right-click to claim dropped items</gray>")
        ))
        meta?.itemModel = NamespacedKey("aprilsteal", "key")
        meta?.persistentDataContainer?.set(keyIdKey, PersistentDataType.STRING, keyId)

        item.itemMeta = meta
        return item
    }

    private fun inventoryFull(player: Player): Boolean {
        return player.inventory.firstEmpty() == -1
    }

    private fun bulkItems(player: Player, bulk: ArrayList<ItemStack>) {
        for (item in bulk) {
            if (!inventoryFull(player)) {
                player.inventory.addItem(item)
            } else {
                player.world.dropItemNaturally(player.location, item)
            }
        }
    }

    private fun saveConfig() {
        try {
            Settings.save()
        } catch (e: Exception) {
            Bukkit.getLogger().warning("Failed to save key system data: ${e.message}")
        }
    }

    // Persistence methods
    private fun persistKeyData(keyId: String, keyData: KeyData) {
        Settings.data.set("keys.$keyId.deadPlayerUuid", keyData.deadPlayerUuid)
        Settings.data.set("keys.$keyId.droppedTime", keyData.droppedTime)
        Settings.data.set("keys.$keyId.location", keyData.location)
        Settings.data.set("inventories.$keyId", keyData.inventory)
        saveConfig()
    }

    private fun removePersistedKeyData(keyId: String) {
        Settings.data.set("keys.$keyId", null)
        Settings.data.set("inventories.$keyId", null)
        saveConfig()
    }

    private fun loadPersistedData() {
        // Load active keys
        val keysSection = Settings.data.getConfigurationSection("keys")
        keysSection?.getKeys(false)?.forEach { keyId ->
            val deadPlayerUuid = Settings.data.getString("keys.$keyId.deadPlayerUuid") ?: return@forEach
            val droppedTime = Settings.data.getLong("keys.$keyId.droppedTime")
            val location = Settings.data.getLocation("keys.$keyId.location")
            val inventory = Settings.data.get("inventories.$keyId") as? Map<Int, ItemStack> ?: emptyMap()

            val keyData = KeyData(keyId, deadPlayerUuid, droppedTime, inventory, location)
            activeKeys[keyId] = keyData
        }

        // Load preserved items
        val preservedItemsSection = Settings.data.getConfigurationSection("preserved_items")
        preservedItemsSection?.getKeys(false)?.forEach { playerUuidStr ->
            val items = Settings.data.get("preserved_items.$playerUuidStr") as? List<ItemStack> ?: return@forEach
            preservedItems[UUID.fromString(playerUuidStr)] = PreservedItems(items)
        }
    }

    private fun saveAllData() {
        activeKeys.forEach { (keyId, keyData) ->
            persistKeyData(keyId, keyData)
        }
    }

    fun shutdown() {
        voidCheckTasks.values.forEach { it.cancel() }
        saveAllData()
    }
}