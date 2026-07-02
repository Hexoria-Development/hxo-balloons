package dev.hexoria.hxo.balloons.paper

import org.bukkit.Material

enum class BalloonColor(val id: String, val displayName: String, val wool: Material) {
    WHITE("white", "§fWeiß", Material.WHITE_WOOL),
    ORANGE("orange", "§6Orange", Material.ORANGE_WOOL),
    MAGENTA("magenta", "§dMagenta", Material.MAGENTA_WOOL),
    LIGHT_BLUE("light_blue", "§bHellblau", Material.LIGHT_BLUE_WOOL),
    YELLOW("yellow", "§eGelb", Material.YELLOW_WOOL),
    LIME("lime", "§aHellgrün", Material.LIME_WOOL),
    PINK("pink", "§dRosa", Material.PINK_WOOL),
    GRAY("gray", "§8Grau", Material.GRAY_WOOL),
    LIGHT_GRAY("light_gray", "§7Hellgrau", Material.LIGHT_GRAY_WOOL),
    CYAN("cyan", "§3Türkis", Material.CYAN_WOOL),
    PURPLE("purple", "§5Lila", Material.PURPLE_WOOL),
    BLUE("blue", "§9Blau", Material.BLUE_WOOL),
    BROWN("brown", "§6Braun", Material.BROWN_WOOL),
    GREEN("green", "§2Grün", Material.GREEN_WOOL),
    RED("red", "§cRot", Material.RED_WOOL),
    BLACK("black", "§0Schwarz", Material.BLACK_WOOL);

    fun variantId(baseId: String): String = "${baseId}_$id"
}
