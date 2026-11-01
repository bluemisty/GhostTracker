plugins {
    kotlin("jvm") version "2.4.21"
    id("net.fabricmc.fabric-loom") version "1.19.3"
}

fun prop(name: String): String = providers.gradleProperty(name).get()

version = prop("mod_version")
group = prop("maven_group")

base {
    archivesName.set(prop("archives_base_name"))
}

repositories {}

dependencies {
    // New toolchain releases use Mojang mappings directly - no remapping layer needed.
    minecraft("com.mojang:minecraft:${prop("minecraft_version")}")
    implementation("net.fabricmc:fabric-loader:${prop("loader_version")}")

    // Fabric API. Technically optional, but the events we use come from it.
    implementation("net.fabricmc.fabric-api:fabric-api:${prop("fabric_api_version")}")

    // Kotlin support. Bundled into the jar so the mod stays standalone.
    implementation("net.fabricmc:fabric-language-kotlin:${prop("fabric_kotlin_version")}")
    include("net.fabricmc:fabric-language-kotlin:${prop("fabric_kotlin_version")}")
}

tasks.withType<ProcessResources>().configureEach {
    val version = prop("mod_version")
    val id = prop("mod_id")
    val name = prop("mod_name")

    inputs.property("version", version)
    inputs.property("id", id)
    inputs.property("name", name)

    filesMatching("fabric.mod.json") {
        expand("version" to version, "id" to id, "name" to name)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 25
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        freeCompilerArgs.addAll("-Xjvm-default=all")
    }
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}
