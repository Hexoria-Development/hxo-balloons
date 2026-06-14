package dev.hiorcraft.hxo.Balloons.balloons

import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.ItemStack

/**
 * Liest und schreibt den Anzeige-[org.bukkit.inventory.ItemStack] eines Ballons unter `Balloons.<id>` in der config.yml.
 *
 * Ballons werden als vollständig serialisierter ItemStack unter `.serialized` gespeichert, was alle
 * Data-Components erhält – inklusive der Spielerkopf-Textur sowie der `item_model`-Component von
 * Custom-Item-Plugins wie Nexo oder Oraxen. Das ältere Format `.item` + `.custommodeldata` wird zur
 * Abwärtskompatibilität weiterhin gelesen.
 */
object BalloonItems {

    fun get(config: ConfigurationSection, balloon: String): ItemStack {
        val base = "Balloons.$balloon"

        // Bevorzugtes Format: über Bukkits ConfigurationSerialization gespeicherter ItemStack.
        config.getItemStack("$base.serialized")?.let { return it.clone() }

        // Legacy-Format: Material + CustomModelData.
        config.getString("$base.item")?.let { materialName ->
            val item = ItemStack(Material.valueOf(materialName))
            val meta = item.itemMeta
            if (meta != null) {
                meta.setCustomModelData(config.getInt("$base.custommodeldata"))
                item.itemMeta = meta
            }
            return item
        }

        // Kein bekanntes Format hinterlegt – Fallback auf einen schlichten Kopf.
        return ItemStack(Material.PLAYER_HEAD)
    }

    fun save(config: ConfigurationSection, balloon: String, item: ItemStack) {
        val base = "Balloons.$balloon"

        // Erst alle Repräsentationen löschen, damit keine veralteten Keys den neuen Wert überschatten.
        config.set("$base.serialized", null)
        config.set("$base.item", null)
        config.set("$base.custommodeldata", null)

        // Bukkit serialisiert den ItemStack inkl. aller Components (auch Kopf-Texturen).
        config.set("$base.serialized", item.clone())
    }
}