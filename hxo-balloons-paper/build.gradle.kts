import dev.slne.surf.api.gradle.util.registerSoft

plugins {
    id("dev.slne.surf.api.gradle.paper-plugin")
}

version = findProperty("version") as String

surfPaperPluginApi {
    mainClass("dev.hexoria.hxo.balloons.paper.PaperMain")
    generateLibraryLoader(false)

    authors.add("HiorCraft")

    serverDependencies {
        registerSoft("Nexo")
    }
}