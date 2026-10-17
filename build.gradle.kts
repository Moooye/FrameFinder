plugins {
    // "fabric-loom" = no-remap 变体（未混淆 MC，main 模块用）
    id("net.fabricmc.fabric-loom") version "1.16-SNAPSHOT" apply false
    // "fabric-loom-remap" = 传统混淆映射变体（main121 / MC 1.21.1 用）
    id("net.fabricmc.fabric-loom-remap") version "1.16-SNAPSHOT" apply false
}
