// gradle.properties 读取助手
fun prop(key: String): String =
    (project.findProperty(key) as String?)?.takeIf { it.isNotBlank() }
        ?: throw GradleException("gradle.properties 缺少属性：$key")

plugins {
    id("java")
    // 1.21.1 是混淆版本，必须用 loom 的 remap 变体（注册 mappings/mod* 配置）
    id("net.fabricmc.fabric-loom-remap")
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

    // 1.21.1 为混淆版本，需要 yarn 映射
    // （loom 1.16 未为 mappings/mod* 生成 Kotlin DSL 类型安全访问器，用字符串形式按名解析）
    "mappings"("net.fabricmc:yarn:${prop("yarn_mappings")}:v2")

    // 1.21.1 下 mod 依赖必须走 modImplementation，让 loom 重映射到 yarn 名
    "modImplementation"("net.fabricmc:fabric-loader:${prop("loader_version")}")
    "modImplementation"("net.fabricmc.fabric-api:fabric-api:${prop("fabric_version")}")

    // ModMenu：可选（提供设置入口），不打包
    "modCompileOnly"("com.terraformersmc:modmenu:${prop("modmenu_version")}")
}

loom {
    runs {
        named("client") {
            ideConfigGenerated(true)
            configName = "FrameFinder 1.21 Client"
            runDir = "../../run121"
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
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
