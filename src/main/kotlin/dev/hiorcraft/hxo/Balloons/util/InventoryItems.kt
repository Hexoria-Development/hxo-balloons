@file:Suppress("DEPRECATION")

package dev.hiorcraft.hxo.Balloons.util

import dev.hiorcraft.hxo.Balloons.PaperMain
import org.bukkit.Material
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

object InventoryItems {

    private val config get() = PaperMain.instance.config

    fun border(inventory: Inventory) {
        for (slot in intArrayOf(45, 46, 47, 51, 52, 53)) {
            val item = ItemStack(Material.BLACK_STAINED_GLASS_PANE)
            val meta = item.itemMeta!!
            meta.setDisplayName(" ")
            item.itemMeta = meta
            inventory.setItem(slot, item)
        }
    }

    fun remove(inventory: Inventory) {
        val item = ItemStack(Material.BARRIER)
        val meta = item.itemMeta!!
        meta.setDisplayName(config.getString("BalloonsMenuRemove"))
        item.itemMeta = meta
        inventory.setItem(49, item)
    }

    fun next(inventory: Inventory) {
        val item = ItemStack(Material.ARROW)
        val meta = item.itemMeta!!
        meta.setDisplayName(config.getString("BalloonsMenuNext"))
        item.itemMeta = meta
        inventory.setItem(50, item)
    }

    fun previous(inventory: Inventory) {
        val item = ItemStack(Material.ARROW)
        val meta = item.itemMeta!!
        meta.setDisplayName(config.getString("BalloonsMenuPrevious"))
        item.itemMeta = meta
        inventory.setItem(48, item)
    }
}
