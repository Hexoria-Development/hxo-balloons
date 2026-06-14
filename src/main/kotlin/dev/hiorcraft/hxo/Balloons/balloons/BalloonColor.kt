package dev.hiorcraft.hxo.Balloons.balloons

import org.bukkit.Material

/**
 * Die 16 Vanilla-Farben. Jede Farbe entspricht einer Nexo-Variante `<basis>_<id>`
 * (z.B. `balloon_red`). [wool] ist das Anzeige-Item im Farbwahl-GUI, [displayName]
 * der bereits eingefärbte Anzeige-Name.
 */
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

    /** Die Nexo-Varianten-ID dieser Farbe für einen Basis-Ballon. */
    fun variantId(baseId: String): String = "${baseId}_$id"
}
