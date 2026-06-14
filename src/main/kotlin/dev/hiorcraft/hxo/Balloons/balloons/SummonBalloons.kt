package dev.hiorcraft.hxo.Balloons.balloons

import dev.hiorcraft.hxo.Balloons.Hook.NexoSupport
import dev.hiorcraft.hxo.Balloons.PaperMain
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Parrot
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.EulerAngle
import org.bukkit.util.Vector
import java.io.File
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

object SummonBalloons {

    /** Höhe des sichtbaren ArmorStands über dem Spieler (in Blöcken). */
    private const val BALLOON_HEIGHT = 3.0
    /** Höhe des Leinen-Ankers (Parrot) – dort hängt der Lead. Etwas über dem Ballon, damit der Strang ihn erreicht. */
    private const val LEASH_HEIGHT = 3.5
    /** Seitlicher Versatz: positiv = links vom Spieler (relativ zur Blickrichtung), in Blöcken. */
    private const val SIDE_OFFSET = 0.8
    /** Lerp-Faktor: Anteil des Wegs zum Ziel pro Tick (0..1). Kleiner = weicher/träger. */
    private const val FOLLOW_SMOOTHING = 0.3
    /** Glättung der Drehung pro Tick (0..1). Kleiner = weicheres/trägeres Drehen. */
    private const val YAW_SMOOTHING = 0.01

    val playerBalloons = HashMap<Player, String>()
    val balloons = HashMap<Player, Parrot>()

    /** Im Original `SummonBalloons.as` – `as` ist in Kotlin reserviert. */
    val armorStands = HashMap<Player, ArmorStand>()
    val percentage = HashMap<Player, Double>()
    val deflateTask = HashMap<Player, BukkitRunnable>()

    /** Pro-Spieler-Status: geglätteter Dreh-Yaw (Radiant), NaN = noch nicht initialisiert. */
    private class Sway {
        var renderYaw = Double.NaN
    }
    private val sway = HashMap<Player, Sway>()

    fun summonBalloon(player: Player, item: ItemStack, percentageBalloon: Double?) {
        val spawnLoc = player.location.clone().add(0.0, 2.0, 0.0)

        // Vor dem Einfügen in die Welt konfigurieren, damit andere Plugins (z.B. Nexo-Möbel)
        // den ArmorStand nicht als "rohen" Default-Stand abgreifen/entfernen.
        val parrot = player.world.spawn(spawnLoc, Parrot::class.java) { p ->
            p.isInvisible = true
            p.isSilent = true
            p.setGravity(false)
            p.setAware(false)
            p.addScoreboardTag("Balloons+")
            p.isInvulnerable = true
        }

        balloons[player] = parrot
        parrot.setLeashHolder(player)

        val stand = player.world.spawn(spawnLoc, ArmorStand::class.java) { s ->
            s.addScoreboardTag("Balloons+")
            s.isVisible = false
            s.setGravity(false)
            s.setCanPickupItems(false)
            s.setArms(true)
            s.setBasePlate(false)
            s.isInvulnerable = true
            s.equipment.setHelmet(item)
            s.addEquipmentLock(EquipmentSlot.HEAD, ArmorStand.LockType.ADDING_OR_CHANGING)
            s.addEquipmentLock(EquipmentSlot.CHEST, ArmorStand.LockType.ADDING_OR_CHANGING)
            s.addEquipmentLock(EquipmentSlot.LEGS, ArmorStand.LockType.ADDING_OR_CHANGING)
            s.addEquipmentLock(EquipmentSlot.FEET, ArmorStand.LockType.ADDING_OR_CHANGING)
            s.addEquipmentLock(EquipmentSlot.HAND, ArmorStand.LockType.ADDING_OR_CHANGING)
            s.addEquipmentLock(EquipmentSlot.OFF_HAND, ArmorStand.LockType.ADDING_OR_CHANGING)
        }

        armorStands[player] = stand
        sway[player] = Sway()

        if (!PaperMain.balloonDoesNotDeflate) {
            stand.isCustomNameVisible = true
            percentage[player] = percentageBalloon ?: 100.0
            val task = object : BukkitRunnable() {
                override fun run() {
                    val current = percentage[player] ?: 0.0
                    if (current > 0.0) {
                        val remaining = current - (PaperMain.numberOfPercentageLostByHour / 60 / 60)
                        stand.customName = "§c" + Math.round(remaining) + "%"
                        percentage[player] = remaining
                    } else {
                        percentage[player] = 0.0
                        removeBalloonWithGiveItem(player)
                        cancel()
                    }
                }
            }
            task.runTaskTimer(PaperMain.instance, 0L, 20L)
            deflateTask[player] = task
        }
    }

    fun updateBalloon(player: Player) {
        val stand = armorStands[player] ?: return
        val parrot = balloons[player] ?: return

        // Weltwechsel -> hart an den Spieler snappen.
        if (stand.world != player.world || parrot.world != player.world) {
            snapTo(player, stand, parrot)
            return
        }

        val s = sway.getOrPut(player) { Sway() }
        val playerLoc = player.location

        // Ziel-Yaw aus Blickrichtung -> geglättet auf kürzestem Weg (sanftes Drehen).
        val facing = playerLoc.direction.setY(0.0)
        val targetYaw = if (facing.lengthSquared() > 0.0) atan2(facing.x, facing.z) else s.renderYaw
        if (s.renderYaw.isNaN()) s.renderYaw = if (targetYaw.isNaN()) 0.0 else targetYaw
        var delta = targetYaw - s.renderYaw
        while (delta > Math.PI) delta -= 2 * Math.PI
        while (delta < -Math.PI) delta += 2 * Math.PI
        if (!delta.isNaN()) s.renderYaw += delta * YAW_SMOOTHING
        val yawRad = s.renderYaw

        // Ziel = Spielerposition + Links-Offset (relativ zum geglätteten Yaw).
        val target = playerLoc.toVector()
        target.add(Vector(cos(yawRad), 0.0, -sin(yawRad)).multiply(SIDE_OFFSET))

        // Basis aus aktueller Stand-Position (minus Höhe) zurückrechnen, dann Richtung Ziel lerpen.
        val base = stand.location.toVector().subtract(Vector(0.0, BALLOON_HEIGHT, 0.0))
        val move = target.clone().subtract(base).multiply(FOLLOW_SMOOTHING)
        base.add(move)

        // Neigung aus dem Bewegungsvektor relativ zum geglätteten Yaw berechnen.
        val c = cos(yawRad)
        val sn = sin(yawRad)
        val localX = move.x * c - move.z * sn
        val localZ = move.x * sn + move.z * c
        val pitch = Math.toRadians(localZ * 50.0 * -1.0)
        val roll = Math.toRadians(localX * 50.0 * -1.0)
        stand.setHeadPose(EulerAngle(pitch, yawRad, roll))

        val world = player.world
        stand.teleport(base.toLocation(world).add(0.0, BALLOON_HEIGHT, 0.0))
        parrot.teleport(base.toLocation(world).add(0.0, LEASH_HEIGHT, 0.0))

        // Sicherheitsnetz: nur bei echtem (horizontalem) Auseinanderdriften snappen,
        // nicht wegen der gewollten Höhe.
        if (base.distance(target) > 8.0) {
            snapTo(player, stand, parrot)
        }
    }

    private fun snapTo(player: Player, stand: ArmorStand, parrot: Parrot) {
        val playerLoc = player.location
        val facing = playerLoc.direction.setY(0.0)
        val target = playerLoc.toVector()
        if (facing.lengthSquared() > 0.0) {
            facing.normalize()
            target.add(Vector(facing.z, 0.0, -facing.x).multiply(SIDE_OFFSET))
        }
        val world = player.world
        stand.teleport(target.toLocation(world).add(0.0, BALLOON_HEIGHT, 0.0))
        parrot.teleport(target.toLocation(world).add(0.0, LEASH_HEIGHT, 0.0))
    }

    /**
     * Legt dem Spieler den Ballon mit der gegebenen ID an (bzw. tauscht das Modell, falls schon einer da ist),
     * wendet dabei die gespeicherte Farbe an und persistiert die Auswahl.
     */
    fun equip(player: Player, balloonId: String) {
        val item = itemForId(balloonId)
        if (balloons.containsKey(player)) {
            armorStands[player]!!.equipment.setHelmet(item)
        } else {
            summonBalloon(player, item, 100.0)
        }
        playerBalloons[player] = balloonId
        BalloonStorage.setBalloon(player.uniqueId, balloonId)
    }

    /** Baut das Anzeige-Item einer Ballon-ID: bevorzugt aus der Config, sonst als Nexo-Item. */
    fun itemForId(balloonId: String): ItemStack {
        val config = YamlConfiguration.loadConfiguration(File(PaperMain.instance.dataFolder, "config.yml"))
        if (config.getConfigurationSection("Balloons")?.getKeys(false)?.contains(balloonId) == true) {
            return BalloonItems.get(config, balloonId)
        }
        return NexoSupport.buildItem(balloonId) ?: ItemStack(Material.BARRIER)
    }

    /** Nexo-Farbvarianten einer Basis-ID (Basis selbst + alle `<basis>_*`), sortiert. */
    fun variantsOf(baseId: String): List<String> {
        val variants = NexoSupport.itemNames().filter { it == baseId || it.startsWith("${baseId}_") }.sorted()
        return variants.ifEmpty { listOf(baseId) }
    }

    fun removeBalloonWithGiveItem(player: Player) {
        val stand = armorStands[player]

        if (PaperMain.balloonWithItemInInventory) {
            percentage.putIfAbsent(player, 100.0)

            val item = stand?.equipment?.helmet
            val meta = item?.itemMeta
            if (meta != null) {
                meta.setDisplayName("§eBalloons+ : " + playerBalloons[player])
                meta.lore = listOf("§6Percentage : " + percentage[player] + "%")
                item.itemMeta = meta
                player.inventory.addItem(item)
            }
        }

        removeBalloon(player)
    }

    fun removeBalloon(player: Player) {
        val stand = armorStands[player] ?: return

        if (PaperMain.instance.config.getBoolean("ShowParticlesBalloonsOnRemove")) {
            stand.world.spawnParticle(Particle.CLOUD, stand.location.clone().add(0.0, 2.0, 0.0), 5, 0.1, 0.1, 0.1, 0.1)
        }
        armorStands.remove(player)
        stand.remove()

        balloons.remove(player)?.remove()
        sway.remove(player)

        if (!PaperMain.balloonDoesNotDeflate) {
            deflateTask.remove(player)?.cancel()
        }
    }

    fun removeAllBalloon() {
        for (player in ArrayList(balloons.keys)) {
            removeBalloonWithGiveItem(player)
        }
        for (parrot in ArrayList(balloons.values)) {
            parrot.remove()
        }
        for (stand in ArrayList(armorStands.values)) {
            if (PaperMain.instance.config.getBoolean("ShowParticlesBalloonsOnRemove")) {
                stand.world.spawnParticle(Particle.CLOUD, stand.location.clone().add(0.0, 2.0, 0.0), 5, 0.1, 0.1, 0.1, 0.1)
            }
            stand.remove()
        }
    }
}