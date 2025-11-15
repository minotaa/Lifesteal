import club.minota.nana.Nana
import club.minota.nana.listeners.CombatTagListener
import club.minota.nana.utils.Settings
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.block.Action
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import org.bukkit.persistence.PersistentDataType
import org.bukkit.NamespacedKey
import java.util.*

data class KeyData(
    val keyId: String,
    val deadPlayerUuid: String,
    val droppedTime: Long,
    val inventory: Map<Int, ItemStack>,
    val pickedUpBy: String? = null,
    val pickupTime: Long? = null
)

data class QueuedInventory(
    val keyId: String
)

data class QueuedKeyRemoval(
    val keyId: String
)

class KeySystem : Listener {
    private val activeKeys = mutableMapOf<String, KeyData>()
    private val despawnTasks = mutableMapOf<String, BukkitTask>()
    private val timeoutTasks = mutableMapOf<String, BukkitTask>()
    private val queuedInventories = mutableMapOf<UUID, MutableList<QueuedInventory>>() // Dead player UUID -> queued inventories
    private val queuedKeyRemovals = mutableMapOf<UUID, MutableList<QueuedKeyRemoval>>() // Picker UUID -> queued key removals
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
        deadPlayer.inventory.contents.forEachIndexed { index, item ->
            item?.let { inventory[index] = it.clone() }
        }

        // Clear the drops so items don't drop normally
        event.drops.clear()

        // Create key data
        val keyId = UUID.randomUUID().toString()
        val keyData = KeyData(
            keyId = keyId,
            deadPlayerUuid = deadPlayer.uniqueId.toString(),
            droppedTime = System.currentTimeMillis(),
            inventory = inventory
        )

        activeKeys[keyId] = keyData
        persistKeyData(keyId, keyData)

        // Drop the key item at death location
        val keyItem = createKeyItem(keyId, deadPlayer.name)
        event.entity.world.dropItemNaturally(event.entity.location, keyItem)

        // Schedule despawn (5 minutes = 300,000ms)
        scheduleDespawn(keyId, deadPlayer.uniqueId)
    }

    @EventHandler
    fun onEntityPickupItem(event: EntityPickupItemEvent) {
        val entity = event.entity
        if (entity !is Player) return

        val item = event.item.itemStack
        if (item.type != Material.SKULL_BANNER_PATTERN) return

        // Check if this is a key item
        val meta = item.itemMeta ?: return
        val keyId = meta.persistentDataContainer.get(keyIdKey, PersistentDataType.STRING) ?: return

        // Check if this key exists
        val keyData = activeKeys[keyId] ?: return

        // If key hasn't been picked up yet, mark it as picked up and start timeout
        if (keyData.pickedUpBy == null) {
            val updatedKeyData = keyData.copy(
                pickedUpBy = entity.uniqueId.toString(),
                pickupTime = System.currentTimeMillis()
            )
            activeKeys[keyId] = updatedKeyData
            persistKeyData(keyId, updatedKeyData)

            // Cancel despawn task and start timeout task (1 hour)
            despawnTasks[keyId]?.cancel()
            despawnTasks.remove(keyId)
            scheduleTimeout(keyId)

            entity.sendMessage(mm.deserialize("<gray>[☠]</gray><white> You picked up a key from </white><yellow>${Bukkit.getOfflinePlayer(UUID.fromString(keyData.deadPlayerUuid)).name}</yellow><white>! Use it within 1 hour or it will be returned.</white>"))
        }
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

        // Cancel any active tasks
        despawnTasks[keyId]?.cancel()
        despawnTasks.remove(keyId)
        timeoutTasks[keyId]?.cancel()
        timeoutTasks.remove(keyId)

        // Remove the key item
        item.amount = 0

        event.player.sendMessage(mm.deserialize("<gray>[☠]</gray><white> You used the key and claimed the </white><green>items</green><white>!</white>"))
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player

        // Process queued key removals first
        queuedKeyRemovals[player.uniqueId]?.let { queuedRemovals ->
            queuedRemovals.forEach { removal ->
                // Remove the key from their inventory
                player.inventory.contents.forEachIndexed { index, item ->
                    if (item != null && item.type == Material.SKULL_BANNER_PATTERN) {
                        val meta = item.itemMeta
                        val storedKeyId = meta?.persistentDataContainer?.get(keyIdKey, PersistentDataType.STRING)
                        if (storedKeyId == removal.keyId) {
                            player.inventory.setItem(index, null)
                        }
                    }
                }
            }
            if (queuedRemovals.isNotEmpty()) {
                player.sendMessage(mm.deserialize("<gray>[☠]</gray><white> Your expired keys have been removed!</white>"))
            }
            queuedKeyRemovals.remove(player.uniqueId)
            Settings.data.set("queued_key_removals.${player.uniqueId}", null)
        }

        // Process queued keys
        queuedInventories[player.uniqueId]?.let { queued ->
            queued.forEach { queuedInventory ->
                val keyData = activeKeys[queuedInventory.keyId]
                if (keyData != null) {
                    // Give the key back to player
                    val keyItem = createKeyItem(queuedInventory.keyId, player.name)
                    player.inventory.addItem(keyItem)
                    player.sendMessage(mm.deserialize("<gray>[☠]</gray><white> Your key was returned to your inventory!</white>"))

                    // Don't clean up the key data - it's still valid
                }
            }
            queuedInventories.remove(player.uniqueId)
            Settings.data.set("queued_inventories.${player.uniqueId}", null)
        }

        // Resume timeout tasks for keys this player picked up
        activeKeys.forEach { (keyId, keyData) ->
            if (keyData.pickedUpBy == player.uniqueId.toString() && keyData.pickupTime != null) {
                val elapsedTime = System.currentTimeMillis() - keyData.pickupTime
                val remainingTime = 3_600_000L - elapsedTime // 1 hour in ms

                if (remainingTime > 0) {
                    val remainingTicks = remainingTime / 50L
                    val task = Bukkit.getScheduler().runTaskLater(Nana.inst, Runnable {
                        returnKeyToDeadPlayer(keyId, "timeout")
                    }, remainingTicks)
                    timeoutTasks[keyId] = task
                } else {
                    // Timeout already expired while offline
                    returnKeyToDeadPlayer(keyId, "timeout")
                }
            }
        }
    }

    private fun scheduleDespawn(keyId: String, deadPlayerUuid: UUID) {
        val task = Bukkit.getScheduler().runTaskLater(Nana.inst, Runnable {
            returnKeyToDeadPlayer(keyId, "despawn")
        }, 6000L) // 5 minutes in ticks (300 seconds * 20 ticks)

        despawnTasks[keyId] = task
    }

    private fun scheduleTimeout(keyId: String) {
        val task = Bukkit.getScheduler().runTaskLater(Nana.inst, Runnable {
            returnKeyToDeadPlayer(keyId, "timeout")
        }, 72000L) // 1 hour in ticks (3600 seconds * 20 ticks)

        timeoutTasks[keyId] = task
    }

    private fun returnKeyToDeadPlayer(keyId: String, reason: String) {
        val keyData = activeKeys[keyId] ?: return
        val deadPlayerUuid = UUID.fromString(keyData.deadPlayerUuid)
        val deadPlayer = Bukkit.getPlayer(deadPlayerUuid)

        // If someone picked up the key, try to remove it from their inventory
        if (keyData.pickedUpBy != null && reason == "timeout") {
            val pickedUpByUuid = UUID.fromString(keyData.pickedUpBy)
            val pickedUpByPlayer = Bukkit.getPlayer(pickedUpByUuid)

            if (pickedUpByPlayer != null && pickedUpByPlayer.isOnline) {
                // Remove the key from their inventory
                pickedUpByPlayer.inventory.contents.forEachIndexed { index, item ->
                    if (item != null && item.type == Material.SKULL_BANNER_PATTERN) {
                        val meta = item.itemMeta
                        val storedKeyId = meta?.persistentDataContainer?.get(keyIdKey, PersistentDataType.STRING)
                        if (storedKeyId == keyId) {
                            pickedUpByPlayer.inventory.setItem(index, null)
                            pickedUpByPlayer.sendMessage(mm.deserialize("<gray>[☠]</gray><white> Your key expired and was returned to the owner!</white>"))
                        }
                    }
                }
            } else {
                // Player is offline, queue the removal for next login
                val queuedRemovals = queuedKeyRemovals.getOrPut(pickedUpByUuid) { mutableListOf() }
                queuedRemovals.add(QueuedKeyRemoval(keyId))
                Settings.data.set("queued_key_removals.$pickedUpByUuid", queuedRemovals)
            }
        }

        if (deadPlayer != null && deadPlayer.isOnline) {
            // Player is online, give key back immediately
            val keyItem = createKeyItem(keyId, deadPlayer.name)
            deadPlayer.inventory.addItem(keyItem)
            deadPlayer.sendMessage(mm.deserialize("<gray>[☠]</gray><white> Your key was returned to your inventory!</white>"))
        } else {
            // Player is offline, queue for next login
            val queued = queuedInventories.getOrPut(deadPlayerUuid) { mutableListOf() }
            queued.add(QueuedInventory(keyId))
            Settings.data.set("queued_inventories.$deadPlayerUuid", queued)
        }

        // Cancel and remove tasks (but don't remove key data - it's still valid)
        despawnTasks[keyId]?.cancel()
        despawnTasks.remove(keyId)
        timeoutTasks[keyId]?.cancel()
        timeoutTasks.remove(keyId)
    }

    private fun createKeyItem(keyId: String, deadPlayerName: String): ItemStack {
        val item = ItemStack(Material.SKULL_BANNER_PATTERN) // Using skull banner pattern as key
        val meta = item.itemMeta
        meta?.displayName(mm.deserialize("<yellow>Key from $deadPlayerName</yellow>"))
        meta?.lore(listOf(
            mm.deserialize("<gray>Key ID: $keyId</gray>"),
            mm.deserialize("<gray>Right-click to claim dropped items</gray>")
        ))
        meta?.itemModel = NamespacedKey("aprilsteal", "key")
        // Store the key ID in persistent data
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

    // Persistence methods
    private fun persistKeyData(keyId: String, keyData: KeyData) {
        Settings.data.set("keys.$keyId.deadPlayerUuid", keyData.deadPlayerUuid)
        Settings.data.set("keys.$keyId.droppedTime", keyData.droppedTime)
        Settings.data.set("keys.$keyId.pickedUpBy", keyData.pickedUpBy)
        Settings.data.set("keys.$keyId.pickupTime", keyData.pickupTime)
        Settings.data.set("inventories.$keyId", keyData.inventory)
    }

    private fun removePersistedKeyData(keyId: String) {
        Settings.data.set("keys.$keyId", null)
        Settings.data.set("inventories.$keyId", null)
    }

    private fun loadPersistedData() {
        // Load active keys
        val keysSection = Settings.data.getConfigurationSection("keys")
        keysSection?.getKeys(false)?.forEach { keyId ->
            val deadPlayerUuid = Settings.data.getString("keys.$keyId.deadPlayerUuid") ?: return@forEach
            val droppedTime = Settings.data.getLong("keys.$keyId.droppedTime")
            val pickedUpBy = Settings.data.getString("keys.$keyId.pickedUpBy")
            val pickupTime = if (Settings.data.contains("keys.$keyId.pickupTime")) {
                Settings.data.getLong("keys.$keyId.pickupTime")
            } else null
            val inventory = Settings.data.get("inventories.$keyId") as? Map<Int, ItemStack> ?: emptyMap()

            val keyData = KeyData(keyId, deadPlayerUuid, droppedTime, inventory, pickedUpBy, pickupTime)
            activeKeys[keyId] = keyData
        }

        // Load queued inventories
        val queuedInventoriesSection = Settings.data.getConfigurationSection("queued_inventories")
        queuedInventoriesSection?.getKeys(false)?.forEach { playerUuidStr ->
            val queued = Settings.data.get("queued_inventories.$playerUuidStr") as? List<QueuedInventory> ?: return@forEach
            queuedInventories[UUID.fromString(playerUuidStr)] = queued.toMutableList()
        }

        // Load queued key removals
        val queuedKeyRemovalsSection = Settings.data.getConfigurationSection("queued_key_removals")
        queuedKeyRemovalsSection?.getKeys(false)?.forEach { playerUuidStr ->
            val queued = Settings.data.get("queued_key_removals.$playerUuidStr") as? List<QueuedKeyRemoval> ?: return@forEach
            queuedKeyRemovals[UUID.fromString(playerUuidStr)] = queued.toMutableList()
        }
    }

    private fun saveAllData() {
        // Ensure all in-memory data is persisted
        activeKeys.forEach { (keyId, keyData) ->
            persistKeyData(keyId, keyData)
        }
    }

    fun shutdown() {
        // Cancel all tasks
        despawnTasks.values.forEach { it.cancel() }
        timeoutTasks.values.forEach { it.cancel() }

        // Save all data before shutdown
        saveAllData()
    }
}