package dev.hexoria.hxo.balloons.paper

import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.UUID

object BalloonStorage {

    private val file: File by lazy { File(PaperMain.instance.dataFolder, "players.yml") }
    private val cfg: YamlConfiguration by lazy { YamlConfiguration.loadConfiguration(file) }

    fun setBalloon(uuid: UUID, balloonId: String) {
        cfg.set(uuid.toString(), balloonId)
        save()
    }

    fun getBalloon(uuid: UUID): String? = cfg.getString(uuid.toString())

    fun remove(uuid: UUID) {
        cfg.set(uuid.toString(), null)
        save()
    }

    private fun save() {
        runCatching { cfg.save(file) }
    }
}