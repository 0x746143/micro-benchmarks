plugins {
    kotlin("jvm") version "2.1.10"
    id("me.champeau.jmh") version "0.7.2"
    kotlin("plugin.allopen") version "2.1.10"
}

group = "x746143"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    implementation(kotlin("reflect"))
    testImplementation(kotlin("test"))
    jmh("org.jetbrains.kotlin:kotlin-test-junit5")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(22)
}

allOpen {
    annotation("org.openjdk.jmh.annotations.State")
}

registerBenchmarkTask("CoroutineDataTransfer")

fun registerBenchmarkTask(className: String, vararg regex: String) {
    tasks.register<JavaExec>(className) {
        val buildDir = project.layout.buildDirectory
        val artifactName = "${project.name}-${project.version}-jmh.jar"
        val jarFile = buildDir.dir("libs/$artifactName")
        val resultFile = buildDir.dir("results/jmh/$className.txt").get()
        dependsOn("jmhJar")
        group = "benchmarks"
        outputs.files(resultFile)
        classpath = files(jarFile)
        mainClass = "org.openjdk.jmh.Main"
        args = mutableListOf<String>().apply {
            if (regex.isEmpty()) {
                add(className)
            } else {
                addAll(regex.map { "$className.*$it.*" })
            }
            addAll(listOf("-foe", "true", "-rf", "text", "-rff", resultFile.toString()))
        }
    }
}
