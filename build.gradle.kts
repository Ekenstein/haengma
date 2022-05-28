import com.github.benmanes.gradle.versions.updates.resolutionstrategy.ComponentSelectionWithCurrent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.nio.file.Paths
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.inputStream

val kotlinVersion by extra("1.6.21")
val junitVersion by extra("5.8.2")

plugins {
    kotlin("jvm") version "1.6.21"
    id("org.jetbrains.dokka") version "1.6.21"
    id("org.jlleitschuh.gradle.ktlint") version "10.3.0"
    id("com.github.ben-manes.versions") version "0.42.0"
    antlr
    `maven-publish`
    jacoco
}

group = "com.github.ekenstein"
version = "2.1.0"
val kotlinJvmTarget = "1.8"

repositories {
    mavenCentral()
}

val dokkaHtml by tasks.getting(org.jetbrains.dokka.gradle.DokkaTask::class)

val javadocJar: TaskProvider<Jar> by tasks.registering(Jar::class) {
    dependsOn(dokkaHtml)
    archiveClassifier.set("javadoc")
    from(dokkaHtml.outputDirectory)
}

dependencies {
    implementation("org.jetbrains.kotlin", "kotlin-stdlib", kotlinVersion)
    testImplementation("org.jetbrains.kotlin", "kotlin-test", kotlinVersion)
    antlr("org.antlr", "antlr4", "4.10.1")

    testImplementation("org.junit.jupiter", "junit-jupiter-params", junitVersion)
    testImplementation("org.junit.jupiter", "junit-jupiter-api", junitVersion)
    testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", junitVersion)
}

tasks {
    dependencyUpdates {
        rejectVersionIf(UpgradeToUnstableFilter())
    }

    val dependencyUpdateSentinel = register<DependencyUpdateSentinel>("dependencyUpdateSentinel", buildDir)
    dependencyUpdateSentinel.configure {
        dependsOn(dependencyUpdates)
    }

    withType<KotlinCompile>() {
        kotlinOptions.jvmTarget = "1.8"
    }

    generateGrammarSource {
        outputDirectory = Paths
            .get("build", "generated-src", "antlr", "main", "com", "github", "ekenstein", "sgf", "parser")
            .toFile()
        mustRunAfter(ktlintMainSourceSetCheck)
        mustRunAfter(dokkaHtml)
    }

    generateTestGrammarSource {
        mustRunAfter(ktlintTestSourceSetCheck)
    }

    kotlinSourcesJar {
        dependsOn("generateGrammarSource")
    }

    compileKotlin {
        dependsOn(generateGrammarSource)
        kotlinOptions {
            jvmTarget = kotlinJvmTarget
            freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn")
        }
    }

    compileTestKotlin {
        dependsOn(generateTestGrammarSource)
        kotlinOptions {
            jvmTarget = kotlinJvmTarget
            freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn")
        }
    }

    compileJava {
        sourceCompatibility = kotlinJvmTarget
        targetCompatibility = kotlinJvmTarget
    }

    listOf(compileJava, compileTestJava).map { task ->
        task {
            sourceCompatibility = kotlinJvmTarget
            targetCompatibility = kotlinJvmTarget
        }
    }

    build {
        dependsOn("javadocJar")
    }

    check {
        dependsOn(test)
        dependsOn(ktlintCheck)
        dependsOn(dependencyUpdateSentinel)
        dependsOn(jacocoTestCoverageVerification)
    }

    test {
        useJUnitPlatform()
        finalizedBy(jacocoTestReport)
    }

    jacocoTestReport {
        dependsOn(test)

        // CSV report for coverage badge
        reports.csv.required.set(true)

        // Exclude generated code from coverage report
        classDirectories.setFrom(
            files(
                classDirectories.files.filter { !it.path.contains("build/classes/java") }.map { file ->
                    fileTree(file).exclude {
                        it.name.contains("special\$\$inlined")
                    }
                }
            )
        )
    }

    jacocoTestCoverageVerification {
        dependsOn(jacocoTestReport)
        violationRules {
            rule {
                limit {
                    minimum = BigDecimal(0.8)
                }
            }
        }
    }
}

ktlint {
    version.set("0.45.2")
}

publishing {
    publications {
        create<MavenPublication>("haengma") {
            groupId = project.group.toString()
            artifactId = "haengma"
            version = project.version.toString()
            from(components["kotlin"])
            artifact(javadocJar)
            artifact(tasks.kotlinSourcesJar)

            pom {
                name.set("haengma")
                description.set("Simple SGF parser")
                url.set("https://github.com/Ekenstein/haengma")
                licenses {
                    license {
                        name.set("MIT")
                        url.set("https://github.com/Ekenstein/haengma/blob/main/LICENSE")
                    }
                }
            }
        }
    }
}

class UpgradeToUnstableFilter : com.github.benmanes.gradle.versions.updates.resolutionstrategy.ComponentFilter {
    override fun reject(cs: ComponentSelectionWithCurrent) = reject(cs.currentVersion, cs.candidate.version)

    private fun reject(old: String, new: String): Boolean {
        return !isStable(new) && isStable(old) // no unstable proposals for stable dependencies
    }

    private fun isStable(version: String): Boolean {
        val stableKeyword = setOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
        val stablePattern = version.matches(Regex("""^[0-9,.v-]+(-r)?$"""))
        return stableKeyword || stablePattern
    }
}

abstract class DependencyUpdateSentinel @Inject constructor(private val buildDir: File) : DefaultTask() {
    @ExperimentalPathApi
    @org.gradle.api.tasks.TaskAction
    fun check() {
        val updateIndicator = "The following dependencies have later milestone versions:"
        val report = Paths.get(buildDir.toString(), "dependencyUpdates", "report.txt")

        report.inputStream().bufferedReader().use { reader ->
            if (reader.lines().anyMatch { it == updateIndicator }) {
                throw GradleException("Dependency updates are available.")
            }
        }
    }
}
