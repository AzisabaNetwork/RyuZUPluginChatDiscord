plugins {
    kotlin("jvm") version "1.9.23"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.23"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "net.azisaba"
version = "1.0.0-SNAPSHOT"

java.toolchain.languageVersion.set(JavaLanguageVersion.of(8))

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.azisaba.net/repository/maven-public/")
}

dependencies {
    val adventureVersion = "4.11.0"
    implementation("dev.kord:kord-core:0.13.1")
    implementation("org.slf4j:slf4j-simple:2.0.1")
    compileOnly("net.azisaba:RyuZUPluginChat:4.5.1") {
        exclude("co.aikar")
    }
    compileOnly("net.azisaba:lunachatplus:3.3.0") {
        exclude("org.bstats")
    }
    //noinspection VulnerableLibrariesLocal
    compileOnly("com.destroystokyo.paper:paper-api:1.15.2-R0.1-SNAPSHOT")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.0.8")
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("com.charleskorn.kaml:kaml:0.53.0") // YAML support for kotlinx.serialization
    // Support for minecraft chat components
    implementation("net.kyori:adventure-api:$adventureVersion")
    implementation("net.kyori:adventure-text-serializer-legacy:$adventureVersion")
    implementation("net.kyori:adventure-text-serializer-plain:$adventureVersion")
}

tasks {
    test {
        useJUnitPlatform()
    }

    shadowJar {
        relocate("dev.kord.", "net.azisaba.ryuzupluginchatdiscord.libs.dev.kord.")
        relocate("com.charleskorn.kaml", "net.azisaba.ryuzupluginchatdiscord.libs.com.charleskorn.kaml")
        relocate("kotlin.", "net.azisaba.ryuzupluginchatdiscord.libs.kotlin.")
        relocate("io.ktor", "net.azisaba.ryuzupluginchatdiscord.libs.io.ktor")
        //relocate("io.github", "net.azisaba.ryuzupluginchatdiscord.libs.io.github")
        relocate("mu", "net.azisaba.ryuzupluginchatdiscord.libs.mu")
        exclude("org/slf4j/**")
        relocate("co.touchlab", "net.azisaba.ryuzupluginchatdiscord.libs.co.touchlab")
        relocate("org.jetbrains", "net.azisaba.ryuzupluginchatdiscord.libs.org.jetbrains")
        relocate("org.intellij", "net.azisaba.ryuzupluginchatdiscord.libs.org.intellij")
        relocate("org.mariadb", "net.azisaba.ryuzupluginchatdiscord.libs.org.mariadb")
        relocate("net.kyori", "net.azisaba.ryuzupluginchatdiscord.libs.net.kyori")
        relocate("kotlinx.", "net.azisaba.ryuzupluginchatdiscord.libs.kotlinx.")
        relocate("com.charleskorn.kaml", "net.azisaba.ryuzupluginchatdiscord.libs.com.charleskorn.kaml")
        relocate("com.zaxxer", "net.azisaba.ryuzupluginchatdiscord.libs.com.zaxxer")
        relocate("org.snakeyaml", "net.azisaba.ryuzupluginchatdiscord.libs.org.snakeyaml")
        relocate("io.github.oshai.kotlinlogging", "net.azisaba.ryuzupluginchatdiscord.libs.io.github.oshai.kotlinlogging")
    }
}

kotlin {
    jvmToolchain(8)
}
