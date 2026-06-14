@file:Suppress("DEPRECATION")

package dev.hiorcraft.hxo.Balloons

import dev.hiorcraft.hxo.Balloons.Hook.NexoSupport
import dev.hiorcraft.hxo.Balloons.balloons.BalloonColor
import dev.hiorcraft.hxo.Balloons.balloons.BalloonItems
import dev.hiorcraft.hxo.Balloons.balloons.BalloonStorage
import dev.hiorcraft.hxo.Balloons.balloons.SummonBalloons
import dev.hiorcraft.hxo.Balloons.util.InventoryItems
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import java.io.File

/** Marker, um das Ballon-Menü zuverlässig zu erkennen (statt fragilem Titel-Vergleich). */
class BalloonMenuHolder : InventoryHolder {
    lateinit var inv: Inventory
    override fun getInventory(): Inventory = inv
}

/**
 * Marker für das Farbwahl-Menü (Stonecutter-Look). Hält die Zuordnung Slot → Nexo-Varianten-ID
 * (nur für klickbare, vorhandene Farben) sowie den Slot des Zurück-Buttons.
 */
class BalloonColorHolder(
    val slotToVariant: Map<Int, String>,
    val backSlot: Int,
) : InventoryHolder {
    lateinit var inv: Inventory
    override fun getInventory(): Inventory = inv
}

object Menu : Listener {

    val pages = HashMap<Player, Int>()
    val list = ArrayList<String>()
    val playerlist = HashMap<Player, ArrayList<String>>()

    private val plugin get() = PaperMain.instance
    private val cfg get() = plugin.config

    fun inventory(player: Player, loop: Int) {
        pages[player] = loop

        val file = File(plugin.dataFolder, "config.yml")
        val config = YamlConfiguration.loadConfiguration(file)

        if (PaperMain.showOnlyBalloonsWithPermission) {
            val visible = ArrayList<String>()
            config.getConfigurationSection("Balloons")?.getKeys(false)?.forEach { key ->
                val perm = config.getString("Balloons.$key.permission")
                if (perm == null || player.hasPermission(perm)) {
                    visible.add(key)
                }
            }
            playerlist[player] = visible

            val title = cfg.getString("BalloonsMenuName") + " (" + ((loop / 45) + 1) + "/" + ((list.size / 45) + 1) + ")"
            val holder = BalloonMenuHolder()
            val inventory = Bukkit.createInventory(holder, 54, title)
            holder.inv = inventory
            player.openInventory(inventory)

            InventoryItems.border(inventory)
            InventoryItems.remove(inventory)
            if (pages[player]!! > 0) InventoryItems.previous(inventory)

            var slot = 0
            for (i in 0 until 45) {
                if (visible.size > i + loop) {
                    val perm = config.getString("Balloons.${visible[i + loop]}.permission")
                    if (perm == null || player.hasPermission(perm)) {
                        val item = BalloonItems.get(config, visible[i + loop])
                        val meta = item.itemMeta!!
                        meta.setDisplayName(
                            config.getString("Balloons.${visible[i + loop]}.displayname") ?: "§e${visible[i + loop]}"
                        )
                        meta.lore = listOf(
                        "",
                        "§6» §eRechtsklick: §7Ballon anlegen",
                        "§6» §eLinksklick: §7Farbe wählen"
                    )
                        item.itemMeta = meta
                        inventory.setItem(slot, item)
                        slot++

                        if (slot == 45) {
                            if (slot != visible.size) InventoryItems.next(inventory)
                            return
                        }
                    }
                }
            }
        } else {
            val title = cfg.getString("BalloonsMenuName") + " (" + ((loop / 45) + 1) + "/" + ((list.size / 45) + 1) + ")"
            val holder = BalloonMenuHolder()
            val inventory = Bukkit.createInventory(holder, 54, title)
            holder.inv = inventory
            player.openInventory(inventory)

            InventoryItems.border(inventory)
            InventoryItems.remove(inventory)
            if (pages[player]!! > 0) InventoryItems.previous(inventory)

            var slot = 0
            for (i in 0 until 45) {
                if (list.size > i + loop) {
                    val item = BalloonItems.get(config, list[i + loop])
                    val meta = item.itemMeta!!
                    meta.setDisplayName(config.getString("Balloons.${list[i + loop]}.displayname") ?: "§e${list[i + loop]}")
                    // Ballons sind für alle nutzbar – immer "Click to summon" anzeigen.
                    meta.lore = listOf(
                        "",
                        "§6» §eRechtsklick: §7Ballon anlegen",
                        "§6» §eLinksklick: §7Farbe wählen"
                    )
                    item.itemMeta = meta
                    inventory.setItem(slot, item)
                    slot++

                    if (slot == 45) {
                        if (slot != list.size) InventoryItems.next(inventory)
                        return
                    }
                }
            }
        }
    }

    /** Slots für die 16 Farb-Buttons (zwei zentrierte Reihen, Stonecutter-Look). */
    private val COLOR_SLOTS = intArrayOf(
        19, 20, 21, 22, 23, 24, 25, 26,
        28, 29, 30, 31, 32, 33, 34, 35,
    )
    private const val PREVIEW_SLOT = 4
    private const val BACK_SLOT = 49

    /**
     * Öffnet die Farbwahl für einen Ballon im Stonecutter-Look: zeigt die 16 Vanilla-Farben.
     * Vorhandene Nexo-Varianten (`<basis>_<farbe>`) sind klickbar, fehlende werden ausgegraut.
     * Gibt es keine einzige Farbvariante, wird der Ballon direkt angelegt.
     */
    fun openVariants(player: Player, baseId: String) {
        val variantSet = SummonBalloons.variantsOf(baseId).toSet()
        val available = BalloonColor.entries.filter { it.variantId(baseId) in variantSet }
        plugin.logger.info("[Balloons-DEBUG] openVariants base='$baseId' nexoPresent=${NexoSupport.isPresent()} available=${available.map { it.id }}")

        if (available.isEmpty()) {
            SummonBalloons.equip(player, baseId)
            player.closeInventory()
            player.sendKosmetika("du hast einen Ballon angelegt.")
            return
        }

        val slotToVariant = HashMap<Int, String>()
        val holder = BalloonColorHolder(slotToVariant, BACK_SLOT)
        val inventory = Bukkit.createInventory(holder, 54, "§eFarbe wählen")
        holder.inv = inventory

        // Hintergrund im hellen Stonecutter-Stil füllen.
        val filler = namedItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE, " ")
        for (slot in 0 until 54) inventory.setItem(slot, filler)

        // Vorschau des Basis-Ballons als "Eingabe" oben.
        inventory.setItem(PREVIEW_SLOT, SummonBalloons.itemForId(baseId))

        // 16 Farben: vorhanden = farbiger Wolle-Button (klickbar), sonst ausgegraut.
        BalloonColor.entries.forEachIndexed { i, color ->
            val slot = COLOR_SLOTS[i]
            if (color in available) {
                val item = namedItem(color.wool, color.displayName)
                val meta = item.itemMeta!!
                meta.lore = listOf("", "§6» §eKlicken: §7Ballon in dieser Farbe anlegen")
                item.itemMeta = meta
                inventory.setItem(slot, item)
                slotToVariant[slot] = color.variantId(baseId)
            } else {
                inventory.setItem(slot, namedItem(Material.GRAY_STAINED_GLASS_PANE, "${color.displayName} §8(nicht verfügbar)"))
            }
        }

        inventory.setItem(BACK_SLOT, namedItem(Material.ARROW, "§7« Zurück"))

        player.openInventory(inventory)
    }

    /** Kleiner Helfer: ItemStack mit gesetztem Anzeige-Namen. */
    private fun namedItem(material: Material, name: String): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta!!
        meta.setDisplayName(name)
        item.itemMeta = meta
        return item
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as Player
        val clicked = event.clickedInventory ?: return
        val slot = event.slot

        // Farbwahl-Menü (Stonecutter-Look): Klick wählt die Farbvariante aus und legt sie an.
        val colorHolder = event.inventory.holder as? BalloonColorHolder
        if (colorHolder != null) {
            event.isCancelled = true
            if (clicked.type == InventoryType.PLAYER) return
            if (slot == colorHolder.backSlot) {
                inventory(player, 0) // Zurück zum Hauptmenü
                return
            }
            val variantId = colorHolder.slotToVariant[slot] ?: return
            SummonBalloons.equip(player, variantId)
            player.closeInventory()
            player.sendKosmetika("du hast einen Ballon angelegt.")
            return
        }

        if (event.inventory.holder !is BalloonMenuHolder) return
        val page = pages[player] ?: 0

        event.isCancelled = true
        val current = event.currentItem ?: return

        // Klick auf einen Ballon: Rechtsklick = anlegen, Linksklick = Farbvariante wählen.
        if (slot < 45 && clicked.type != InventoryType.PLAYER) {
            val id = balloonIdAt(player, slot + page)
            if (id != null) {
                if (event.isRightClick) {
                    SummonBalloons.equip(player, id)
                    player.closeInventory()
                    player.sendKosmetika("du hast einen Ballon angelegt.")
                } else {
                    openVariants(player, id)
                }
                return
            }
        }

        // Entfernen-Button.
        if (current.type == Material.BARRIER) {
            player.closeInventory()
            val had = SummonBalloons.balloons.containsKey(player)
            SummonBalloons.removeBalloonWithGiveItem(player)
            SummonBalloons.playerBalloons.remove(player)
            BalloonStorage.remove(player.uniqueId)
            if (had) player.sendKosmetika("du hast deinen Ballon abgenommen.")
        }
        if (slot == 48) inventory(player, page - 45)
        if (slot == 50) inventory(player, page + 45)
    }

    /** Liefert die Ballon-ID an der gegebenen Menü-Position, oder null. */
    private fun balloonIdAt(player: Player, index: Int): String? {
        if (PaperMain.showOnlyBalloonsWithPermission) {
            return playerlist[player]?.getOrNull(index)
        }
        val file = File(plugin.dataFolder, "config.yml")
        val config = YamlConfiguration.loadConfiguration(file)
        val keys = config.getConfigurationSection("Balloons")?.getKeys(false)?.toList() ?: return null
        return keys.getOrNull(index)
    }
}
