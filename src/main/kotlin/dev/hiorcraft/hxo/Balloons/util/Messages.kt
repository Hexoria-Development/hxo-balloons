package dev.hiorcraft.hxo.Balloons

import dev.slne.surf.api.core.messages.adventure.sendText
import org.bukkit.entity.Player

fun Player.sendKosmetika(text: String) {
    sendText {
        warning("[Kosmetika]")
        appendSpace()
        darkSpacer("|")
        appendSpace()
        info(text)
    }
}
