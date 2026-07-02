package dev.hexoria.hxo.balloons.paper

import dev.hexoria.hxo.balloons.paper.listener.BalloonListener
import dev.hexoria.hxo.balloons.paper.gui.Menu
import dev.hexoria.hxo.balloons.paper.command.balloonsCommand
import org.bukkit.Bukkit
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Parrot
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable

class PaperMain : JavaPlugin() {

    companion object {
        lateinit var instance: PaperMain
            private set

        var showOnlyBalloonsWithPermission = false
        var balloonDoesNotDeflate = true
        var balloonWithItemInInventory = false
        var numberOfPercentageLostByHour = 0.0
        var numberOfPercentageInflateByHour = 0.0
        lateinit var prefix: String
    }

    override fun onEnable() {
        instance = this

        val pm = server.pluginManager
        pm.registerEvents(BalloonListener(), this)
        pm.registerEvents(Menu, this)

        for (world in Bukkit.getWorlds()) {
            for (entity in world.entities) {
                if (entity is Parrot && entity.scoreboardTags.contains("Balloons+")) {
                    entity.remove()
                }
                if (entity is ArmorStand && entity.scoreboardTags.contains("Balloons+")) {
                    entity.remove()
                }
            }
        }

        balloonsCommand()

        object : BukkitRunnable() {
            override fun run() {
                for (player in ArrayList(SummonBalloons.balloons.keys)) {
                    SummonBalloons.updateBalloon(player)
                }
            }
        }.runTaskTimer(this, 0L, 1L)


        config.options().copyDefaults(true)
        saveConfig()

        config.getConfigurationSection("Balloons")?.let { Menu.list.addAll(it.getKeys(false)) }


        logger.info("Balloons enabled !")

        object : BukkitRunnable() {
            override fun run() {
                for (player in Bukkit.getOnlinePlayers()) {
                    for (i in 0 until 36) {
                        val itemStack = player.inventory.getItem(i) ?: continue
                        if (!itemStack.hasItemMeta()) continue
                        val meta = itemStack.itemMeta ?: continue
                        if (!meta.hasDisplayName()) continue
                        if (!meta.displayName.contains("§eBalloons : ")) continue

                        val percentage = meta.lore!![0].split(" : ")[1].replace("%", "")
                        var newPercentage = percentage.toDouble() + (numberOfPercentageInflateByHour / 60 / 60)
                        if (newPercentage > 100) newPercentage = 100.0

                        val clone = itemStack.clone()
                        val cloneMeta = clone.itemMeta!!
                        cloneMeta.lore = listOf("§6Percentage : $newPercentage%")
                        clone.itemMeta = cloneMeta
                        player.inventory.setItem(i, clone)
                    }
                }
            }
        }.runTaskTimer(this, 0L, 20L)
    }

    override fun onDisable() {
        SummonBalloons.removeAllBalloon()
        logger.info("Balloons disabled !")
    }
}