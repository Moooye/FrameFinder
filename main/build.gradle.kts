// gradle.properties 读取助手
fun prop(key: String): String =
    (project.findProperty(key) as String?)?.takeIf { it.isNotBlank() }
        ?: throw GradleException("gradle.properties 缺少属性：$key")

plugins {
    id("java")
    id("net.fabricmc.fabric-loom")
}

version = prop("mod_version")
group = prop("mod_maven_group")

base {
    archivesName.set(prop("mod_archives_base_name"))
}

repositories {
    mavenCentral()
    maven("https://maven.terraformersmc.com/releases/")
}

dependencies {
    minecraft("com.mojang:minecraft:${prop("minecraft_version")}")

    implementation("net.fabricmc:fabric-loader:${prop("loader_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${prop("fabric_version")}")

    // ModMenu：可选（提供设置入口），不打包
    compileOnly("com.terraformersmc:modmenu:${prop("modmenu_version")}")
}

loom {
    runs {
        named("client") {
            ideConfigGenerated(true)
            configName = "FrameFinder Client"
            runDir = "../../run"
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.processResources {
    val props = mapOf(
        "mod_id" to prop("mod_id"),
        "mod_name" to prop("mod_name"),
        "mod_version" to prop("mod_version"),
        "mod_description" to prop("mod_description"),
        "minecraft_dependency" to prop("minecraft_dependency"),
        "java_version" to prop("java_version")
    )
    inputs.properties(props)
    filesMatching(listOf("fabric.mod.json")) {
        expand(props)
    }
}

tasks.register<Copy>("buildAndCollect") {
    group = "build"
    from(tasks.named("jar"))
    into(rootProject.layout.buildDirectory.dir("libs"))
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}
