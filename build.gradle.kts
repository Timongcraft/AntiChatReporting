plugins {
    id("java-library")
    id("io.papermc.paperweight.userdev") version "1.3.7"
    id("xyz.jpenilla.run-paper") version "1.0.6"
    id("net.minecrell.plugin-yml.bukkit") version "0.5.2"
}

group = "timongcraft.antichatreporting"
version = "1.2"
description = "A AntiChatReport Plugin with more options"

dependencies {
    paperDevBundle("1.19.1-R0.1-SNAPSHOT")
}

tasks {
    assemble {
        dependsOn(reobfJar)
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name()
    }
}

bukkit {
    authors = listOf("Oharass", "sulu", "Timongcraft")
    main = "timongcraft.antichatreporting.Main"
    apiVersion = "1.19"
}
