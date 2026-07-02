package dev.hexoria.hxo.balloons.paper.hook

import dev.hexoria.hxo.balloons.paper.PaperMain
import org.bukkit.inventory.ItemStack

/**
 * Schmaler, reflektiver Zugriff auf die Nexo-API (`com.nexomc.nexo.api.NexoItems`).
 * Bewusst per Reflection, damit das Plugin ohne Nexo-Compile-Dependency baut und
 * versions-unabhängig bleibt – fehlt Nexo, liefern die Methoden leere Werte/null.
 */
object NexoSupport {

    private val nexoItemsClass: Class<*>? by lazy {
        runCatching { Class.forName("com.nexomc.nexo.api.NexoItems") }.getOrNull()
    }

    /** Instanz des Kotlin-`object` NexoItems (Feld INSTANCE), falls vorhanden. */
    private val instance: Any? by lazy {
        nexoItemsClass?.let { runCatching { it.getField("INSTANCE").get(null) }.getOrNull() }
    }

    fun isPresent(): Boolean = nexoItemsClass != null

    /** Alle bekannten Nexo-Item-IDs. */
    fun itemNames(): List<String> {
        val clazz = nexoItemsClass
        if (clazz == null) {
            PaperMain.instance.logger.warning("[Balloons-DEBUG] NexoItems-Klasse nicht gefunden (Nexo geladen?)")
            return emptyList()
        }
        val result = try {
            clazz.getMethod("itemNames").invoke(instance)
        } catch (e: Throwable) {
            PaperMain.instance.logger.warning("[Balloons-DEBUG] itemNames-Reflection fehlgeschlagen: ${e.message} cause=${e.cause}")
            return emptyList()
        }
        val list = when (result) {
            is Array<*> -> result.mapNotNull { it?.toString() }
            is Collection<*> -> result.mapNotNull { it?.toString() }
            else -> {
                PaperMain.instance.logger.warning("[Balloons-DEBUG] itemNames lieferte unerwarteten Typ: ${result?.javaClass}")
                emptyList()
            }
        }
        PaperMain.instance.logger.info("[Balloons-DEBUG] Nexo itemNames count=${list.size}")
        return list
    }

    /** Baut das ItemStack einer Nexo-Item-ID (oder null, wenn unbekannt / Nexo fehlt). */
    fun buildItem(id: String): ItemStack? {
        val clazz = nexoItemsClass ?: return null
        val builder = runCatching {
            clazz.getMethod("itemFromId", String::class.java).invoke(instance, id)
        }.getOrNull() ?: return null
        return runCatching { builder.javaClass.getMethod("build").invoke(builder) as? ItemStack }.getOrNull()
    }

    fun exists(id: String): Boolean = itemNames().contains(id)
}