import org.kingdoms.gradle.KingdomsGradleCommons.localDependency

plugins {
    commons
    spigotapi
}

group = "org.kingdoms.services"
version = "1.0.0"

repositories {
    maven { url = uri("https://repo.codemc.org/repository/nms/") }
    maven { url = uri("https://libraries.minecraft.net/") }
    maven {
        name = "BSRepository"
        url = uri("https://repo.bsdevelopment.org/releases")
    }
}

dependencies {
    compileOnly(project(":core:service"))

    // They don't have an official API.
    compileOnly(localDependency("MCPets-3.0.2.jar"))

    // Local https://wiki.mypet-plugin.de/hooks/hook-types
    compileOnly(localDependency("MyPet-3.12-SNAPSHOT-B1727.jar"))

    // https://discord.com/channels/903053383475277844/1203319394499829770/1206931275060084767
    // Not using NexEngine will result in compile errors: error: cannot access AbstractManager
    // https://github.com/nulli0n/NexEngine-spigot/releases
    compileOnly(localDependency("CombatPets-1.0.30.jar"))
    compileOnly(localDependency("NexEngine-2.2.12-R2-Final.jar"))

    // https://github.com/brainsynder-Dev/SimplePets
    // https://www.spigotmc.org/resources/100106/
    // https://repo.bsdevelopment.org/#/releases/simplepets/brainsynder/API
    compileOnly("simplepets.brainsynder:API:5.0-BUILD-272") { isTransitive = false }
}