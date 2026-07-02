package dev.hexoria.hxo.balloons.paper.listener

import dev.hexoria.hxo.balloons.paper.hook.NexoSupport
import dev.hexoria.hxo.balloons.paper.PaperMain
import dev.hexoria.hxo.balloons.paper.BalloonStorage
import dev.hexoria.hxo.balloons.paper.SummonBalloons
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Item
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDismountEvent
import org.bukkit.event.entity.EntityMountEvent
import org.bukkit.event.entity.EntityUnleashEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.entity.PlayerLeashEntityEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.event.player.PlayerUnleashEntityEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.scheduler.BukkitRunnable
import java.io.File

class BalloonListener : Listener {

    private companion object {
        const val BALLOON_TAG = "Balloons+"
    }

    /** Schützt Ballon-Entities (Parrot + ArmorStand) vor jeglichem Schaden. */
    @EventHandler
    fun onEntityDamage(event: EntityDamageEvent) {
        if (event.entity.scoreboardTags.contains(BALLOON_TAG)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onTeleport(event: PlayerTeleportEvent) {
        val player = event.player
        if (!SummonBalloons.balloons.containsKey(player)) return

        val stand = SummonBalloons.armorStands[player]!!
        val item = stand.equipment.helmet

        SummonBalloons.armorStands.remove(player)
        stand.remove()

        val parrot = SummonBalloons.balloons[player]!!
        SummonBalloons.balloons.remove(player)
        parrot.remove()

        object : BukkitRunnable() {
            override fun run() {
                SummonBalloons.summonBalloon(player, item, SummonBalloons.percentage[player])
            }
        }.runTaskLater(PaperMain.instance, 10L)
    }

    @EventHandler
    fun onDisconnect(event: PlayerQuitEvent) {
        removeFor(event.player)
    }

    @EventHandler
    fun onDeath(event: PlayerDeathEvent) {
        removeFor(event.entity)
    }

    /** Beim Joinen den zuletzt getragenen Ballon wiederherstellen. */
    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        restoreBalloon(event.player)
    }

    /** Nach dem Respawnen den getragenen Ballon wiederherstellen. */
    @EventHandler
    fun onRespawn(event: PlayerRespawnEvent) {
        restoreBalloon(event.player)
    }

    private fun restoreBalloon(player: Player) {
        if (PaperMain.balloonWithItemInInventory) return
        val id = BalloonStorage.getBalloon(player.uniqueId) ?: return

        object : BukkitRunnable() {
            override fun run() {
                if (!player.isOnline || SummonBalloons.balloons.containsKey(player)) return

                val file = File(PaperMain.instance.dataFolder, "config.yml")
                val config = YamlConfiguration.loadConfiguration(file)
                val inConfig = config.getConfigurationSection("Balloons")?.getKeys(false)?.contains(id) == true
                if (!inConfig && !NexoSupport.exists(id)) {
                    BalloonStorage.remove(player.uniqueId)
                    return
                }

                SummonBalloons.equip(player, id)
            }
        }.runTaskLater(PaperMain.instance, 20L)
    }

    /** Verhindert, dass Spieler den Ballon (an-)leinen. */
    @EventHandler
    fun onLeash(event: PlayerLeashEntityEvent) {
        if (event.entity.scoreboardTags.contains(BALLOON_TAG)) {
            event.isCancelled = true
        }
    }

    /** Verhindert manuelles Ableinen des Ballons durch den Spieler. */
    @EventHandler
    fun onUnleash(event: PlayerUnleashEntityEvent) {
        if (event.entity.scoreboardTags.contains(BALLOON_TAG)) {
            event.isCancelled = true
        }
    }

    /**
     * Fängt das automatische Ableinen ab (z.B. Halter weg / zu weit entfernt):
     * entfernt das fallengelassene Lead-Item und leint den Ballon wieder an
     * den Halter – bzw. an den Besitzer aus der Ballon-Map.
     */
    @EventHandler
    fun onEntityUnleash(event: EntityUnleashEvent) {
        val living = event.entity as? LivingEntity ?: return
        if (!living.scoreboardTags.contains(BALLOON_TAG)) return

        val holder = if (living.isLeashed) living.leashHolder else null

        Bukkit.getScheduler().runTask(PaperMain.instance, Runnable {
            // Das beim Ableinen gedroppte Lead entfernen.
            living.getNearbyEntities(15.0, 15.0, 15.0)
                .filterIsInstance<Item>()
                .firstOrNull { it.itemStack.type == Material.LEAD }
                ?.remove()

            if (holder != null && holder.isValid) {
                runCatching {
                    living.teleport(holder.location)
                    living.setLeashHolder(holder)
                }
                return@Runnable
            }

            val owner = SummonBalloons.balloons.entries.firstOrNull { it.value == living }?.key
            if (owner != null && owner.isOnline) {
                runCatching {
                    living.teleport(owner.location)
                    living.setLeashHolder(owner)
                }
            }
        })
    }

    /** Verhindert Rechtsklick-Interaktion mit Ballon-Entities (Parrot + ArmorStand). */
    @EventHandler
    fun onInteract(event: PlayerInteractAtEntityEvent) {
        if (event.rightClicked.scoreboardTags.contains(BALLOON_TAG)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onMount(event: EntityMountEvent) {
        val player = event.entity
        if (player is Player && SummonBalloons.balloons.containsKey(player)) {
            removeFor(player)
        }
    }

    @EventHandler
    fun onDismount(event: EntityDismountEvent) {
        val player = event.entity
        if (player !is Player) return

        if (SummonBalloons.playerBalloons.containsKey(player) && !PaperMain.balloonWithItemInInventory) {
            if (SummonBalloons.armorStands[player] == null) {
                SummonBalloons.equip(player, SummonBalloons.playerBalloons[player]!!)
            }
        }
    }

    @EventHandler
    fun onPlayerClicks(event: PlayerInteractEvent) {
        val player = event.player
        val action = event.action
        val item = event.item

        if (!PaperMain.balloonWithItemInInventory) return
        if (player.isInsideVehicle) return
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return

        val meta = item?.itemMeta ?: return
        if (!meta.hasDisplayName()) return
        if (!meta.displayName.contains("§eBalloons+ : ")) return

        event.isCancelled = true
        if (SummonBalloons.balloons.containsKey(player)) return

        SummonBalloons.playerBalloons[player] = meta.displayName.split(" : ")[1]
        val percentageBalloon = meta.lore!![0].split(" : ")[1].replace("%", "")

        if (percentageBalloon.toDouble() > 0.0) {
            SummonBalloons.summonBalloon(player, player.equipment.itemInMainHand, percentageBalloon.toDouble())
            player.equipment.setItem(EquipmentSlot.HAND, null)
        }
    }

    private fun removeFor(player: Player) {
        if (!SummonBalloons.balloons.containsKey(player)) return
        if (PaperMain.balloonWithItemInInventory) {
            SummonBalloons.removeBalloonWithGiveItem(player)
        } else {
            SummonBalloons.removeBalloon(player)
        }
    }
}