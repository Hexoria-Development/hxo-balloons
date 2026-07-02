@file:Suppress("DEPRECATION")

package dev.hexoria.hxo.balloons.paper.command

import dev.hexoria.hxo.balloons.paper.BalloonItems
import dev.hexoria.hxo.balloons.paper.BalloonStorage
import dev.hexoria.hxo.balloons.paper.PaperMain
import dev.hexoria.hxo.balloons.paper.SummonBalloons
import dev.hexoria.hxo.balloons.paper.gui.Menu
import dev.hexoria.hxo.balloons.paper.util.sendKosmetika
import dev.jorel.commandapi.kotlindsl.commandTree
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import org.bukkit.ChatColor
import org.bukkit.NamespacedKey
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import java.io.File

private val plugin get() = PaperMain.instance
private val cfg get() = plugin.config

fun balloonsCommand() = commandTree("balloons") {
    playerExecutor { player, _ -> openDefault(player) }


    literalArgument("reload") {
        withPermission("balloons.reload")
        playerExecutor { player, _ ->
            reload()
            player.sendMessage(PaperMain.prefix + cfg.getString("BalloonReload"))
        }
    }

    literalArgument("inventory") {
        playerExecutor { player, _ -> openDefault(player) }
    }

    literalArgument("remove") {
        playerExecutor { player, _ ->
            if (SummonBalloons.balloons.containsKey(player)) {
                SummonBalloons.removeBalloonWithGiveItem(player)
                SummonBalloons.playerBalloons.remove(player)
                BalloonStorage.remove(player.uniqueId)
                player.sendKosmetika("du hast deinen Ballon abgenommen.")
            }
        }
    }

    literalArgument("create") {
        playerExecutor { player, _ ->
            val item = player.inventory.itemInMainHand
            if (item.type.isAir) {
                player.sendKosmetika("du musst ein Item in der Hand halten, um einen Ballon zu erstellen.")
                return@playerExecutor
            }

            val meta = item.itemMeta
            val nexoKey = NamespacedKey.fromString("nexo:id")
            val nexoId = if (nexoKey != null && meta != null) {
                meta.persistentDataContainer.get(nexoKey, PersistentDataType.STRING)
            } else null
            val rawName = when {
                nexoId != null -> nexoId
                meta != null && meta.hasDisplayName() -> meta.displayName
                else -> item.type.name
            }
            val id = (ChatColor.stripColor(rawName) ?: rawName).trim().replace(" ", "_").ifEmpty { item.type.name }

            val file = File(plugin.dataFolder, "config.yml")
            val config = YamlConfiguration.loadConfiguration(file)
            BalloonItems.save(config, id, item.clone())
            config.set("Balloons.$id.displayname", rawName)
            config.save(file)
            reload()

            player.sendKosmetika("du hast den Ballon '$id' erstellt.")
        }
    }
}

private fun openDefault(player: Player) {
    if (PaperMain.balloonWithItemInInventory) {
        player.sendMessage(PaperMain.prefix + cfg.getString("CantOpenInventoryWithBalloonWithItemInInventory"))
    } else if (player.isInsideVehicle) {
        player.sendMessage(PaperMain.prefix + cfg.getString("CantOpenInventory"))
    } else {
        Menu.inventory(player, 0)
    }
}

fun reload() {
    val config = plugin.config
    config.load(File(plugin.dataFolder, "config.yml"))

    Menu.list.clear()
    config.getConfigurationSection("Balloons")?.let { Menu.list.addAll(it.getKeys(false)) }

    PaperMain.showOnlyBalloonsWithPermission = config.getBoolean("ShowOnlyBalloonsWithPermission")
    PaperMain.prefix = config.getString("BalloonPrefix") ?: "§b[Balloons+] "
}