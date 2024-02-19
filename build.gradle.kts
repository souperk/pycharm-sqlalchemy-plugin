
fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)


plugins {
    // Kotlin support
    kotlin("jvm") version "1.9.22"
    // Gradle IntelliJ Plugin
    id("org.jetbrains.intellij") version "1.17.0"
    // Gradle Changelog Plugin
    id("org.jetbrains.changelog") version "2.2.0"
}

group = properties("pluginGroup").get()
version = properties("pluginVersion").get()

repositories {
    mavenCentral()
}

// Set the JVM language level used to build the project. Use Java 17 for 2022.2+.
kotlin {
    jvmToolchain(17)
}

intellij {
    pluginName.set(properties("pluginName"))
    version.set(properties("platformVersion"))
    type.set(properties("platformType"))

    // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file.
    plugins.set(properties("platformPlugins").map { it.split(',').map(String::trim).filter(String::isNotEmpty) })
}

tasks {
    wrapper {
        gradleVersion = properties("gradleVersion").get()
    }


    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "17"
        }
    }
}

dependencies {
    compileOnly("org.apache.tuweni:tuweni-toml:2.3.1")
    compileOnly(group = "org.ini4j", name = "ini4j", version = "0.5.4")
    testImplementation(kotlin("test"))
}

sourceSets {
    main {
        java.srcDir("src")
        resources.srcDir("resources")
    }
}
