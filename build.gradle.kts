import dev.slne.surf.api.gradle.util.registerSoft

plugins {
    id("dev.slne.surf.api.gradle.paper-plugin")
}

version = findProperty("version") as String
group = "de.hiorcraft.nex"

surfPaperPluginApi {
    mainClass("dev.hiorcraft.hxo.Balloons.PaperMain")
    generateLibraryLoader(false)

    authors.add("HiorCraft")

    // Nexo läuft als eigenes Plugin mit isoliertem Classloader. Ohne diese
    // Abhängigkeit sieht hxo-balloons die Klasse com.nexomc.nexo.api.NexoItems
    // nicht (Class.forName -> ClassNotFoundException, nexoPresent=false).
    // Soft = required:false, damit das Plugin auch ohne Nexo startet.
    serverDependencies {
        registerSoft("Nexo")
    }
}