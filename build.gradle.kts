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

abstract class ExecCommand : DefaultTask() {
    @get:Inject
    abstract val execOperations: ExecOperations

    fun execCommand(vararg args: String): ExecResult {
        return execOperations.exec {
            commandLine(*args)
        }
    }
}

tasks["compileJmhKotlin"].dependsOn("compileJmhNative")

tasks.register<ExecCommand>("compileJmhNative") {
    val javaHome: String = System.getProperty("java.home")
    val nativeDir = layout.buildDirectory.dir("native").get()
    val sourceDir = layout.projectDirectory.dir("src/jmh/c")
    val sourceFiles = fileTree(sourceDir) { include("**/*.c", "**/*.h") }
    val libFiles = sourceFiles
        .filter { it.name.startsWith("lib") }
        .associate { src -> ("$nativeDir/${src.name.replace(".c", ".so")}") to src.absolutePath }

    inputs.files(sourceFiles)
    outputs.files(libFiles.keys)

    doLast {
        libFiles.forEach { (outFile, srcFile) ->
            execCommand(
                "gcc", "-shared", "-fPIC", "-O3", "-flto",
                "-I$javaHome/include",
                "-I$javaHome/include/linux",
                "-I$sourceDir/openjdk/libjava",
                "-o", outFile, srcFile
            )
        }
    }
}
