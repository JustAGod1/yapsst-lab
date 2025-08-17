import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.0"
    application
    antlr
}

group = "org.stella"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    antlr("org.antlr:antlr4:4.9.3")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    implementation(kotlin("script-runtime"))
}

val bnfcGenDir = rootProject.file("src/main/antlr").absoluteFile
val antlrGenDir = layout.buildDirectory.file("generated-src/antlr").get().asFile.absoluteFile

println("bnfc = ${bnfcGenDir.path}")
println("antlr = ${antlrGenDir.path}")

sourceSets.main {
    antlr.srcDir(bnfcGenDir)
    java.srcDir(bnfcGenDir)
}

val genAntlrGrammar = tasks.create<Exec>("genAntlr4Grammar") {
    val inputFile = rootProject.file("Stella.cf").absoluteFile
    commandLine("./bnfc-2.9.6.1", "--java", "-o", bnfcGenDir, "--antlr4", "-p", "org.syntax", inputFile.absolutePath)
    inputs.file(inputFile)
    outputs.dir(bnfcGenDir)

    doLast {
        bnfcGenDir.resolve("org/syntax/stella/Test.java").delete()
    }
}

tasks.generateGrammarSource {
    maxHeapSize = "64m"
    //outputDirectory = antlrGenDir
    arguments = arguments + listOf("-Xexact-output-dir", "-visitor", "-package", "org.syntax")
    dependsOn(genAntlrGrammar)
    inputs.files(genAntlrGrammar)
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    inputs.files(tasks.generateGrammarSource)
    inputs.files(tasks.generateTestGrammarSource)
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("org.stella.MainKt")
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}
