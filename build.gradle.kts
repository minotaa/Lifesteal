plugins {
    id("org.jetbrains.kotlin.jvm") version "2.1.20"
    id("io.github.goooler.shadow") version "8.1.8"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
    java
}
group = "club.minota"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
}

dependencies {
    paperweight.paperDevBundle("1.21.10-R0.1-SNAPSHOT")
}