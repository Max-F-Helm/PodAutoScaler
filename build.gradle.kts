import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    id("org.springframework.boot") version "2.6.4"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"

    id("net.mayope.deployplugin") version "0.0.51"

    kotlin("jvm") version "1.6.10"
    kotlin("plugin.spring") version "1.6.10"

    id("com.diffplug.spotless") version "6.3.0"
    id("io.gitlab.arturbosch.detekt") version "1.19.0"
    id("com.github.ben-manes.versions") version "0.42.0"
    id("org.owasp.dependencycheck") version "7.0.1"
}

group = "com.mfhelm.podautoscaler"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
    mavenCentral()
}

dependencies {
    // Kotlin
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-amqp")
    implementation("io.fabric8:kubernetes-client:5.12.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.2")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.13.2")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(module = "mockito-core")
    }
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.6.10")

    // Mocking
    testImplementation("io.mockk:mockk:1.12.3")
    testImplementation("org.assertj:assertj-core:3.22.0")
    testImplementation("com.ninja-squad:springmockk:3.1.1")
}

deploy {
    serviceName = "podautoscaler"
    default {
        dockerBuild {
        }
        dockerLogin {
            registryRoot = property("registryRoot").toString()
            loginMethod = net.mayope.deployplugin.tasks.DockerLoginMethod.DOCKERHUB
        }
        dockerPush {
            registryRoot = property("registryRoot").toString()
            loginMethod = net.mayope.deployplugin.tasks.DockerLoginMethod.DOCKERHUB
            loginUsername = property("dockerUser").toString()
            loginPassword = property("dockerPwd").toString()
        }
        deploy {
            targetNamespaces = listOf("integration")
        }
        helmPush {
            repositoryUrl = property("helmRepo").toString()
            repositoryUsername = property("helmUser").toString()
            repositoryPassword = property("helmPwd").toString()
        }
    }
}

val ktLintVersion = "0.45.1"
spotless {
    kotlin {
        ktlint(ktLintVersion)
        target(
            "src/main/kotlin/**/*.kt",
            "src/test/kotlin/**/*.kt",
            "src/main/java/**/*.kt",
            "src/test/java/**/*.kt"
        )
    }
    kotlinGradle {
        ktlint(ktLintVersion)
        targetExclude("build/**")
        target("build.gradle.kts")
    }
}

dependencyCheck {
    failOnError = true
    outputDirectory = "$buildDir/reports/dependencyCheck"
    analyzers.assemblyEnabled = false

    // https://www.first.org/cvss/specification-document#Qualitative-Severity-Rating-Scale
    failBuildOnCVSS = 7.0f
}

tasks {
    named<Detekt>("detekt") {
        jvmTarget = "11"
        parallel = true
        buildUponDefaultConfig = true

        setSource(files(projectDir))
        include("**/*.kt")
        include("**/*.kts")
        exclude("**/resources/**")
        exclude("**/build/**")

        reports {
            html.required.set(true)
            html.outputLocation.set(file("$buildDir/reports/detekt/report.html"))
            txt.required.set(false)
            xml.required.set(false)
            sarif.required.set(false)
        }
    }

    named<DependencyUpdatesTask>("dependencyUpdates") {
        group = "verification"
        description = "Checks if dependencies are up-to-date"

        // exclude release candidates, etc
        rejectVersionIf {
            candidate.version.matches(Regex(".*-RC\\d?")) ||
                    candidate.version.matches(Regex(".*-M\\d?"))
        }

        outputDir = "$buildDir/reports/dependencyUpdates"
        reportfileName = "updates"
    }

    named<Jar>("jar") {
        enabled = true
        classifier = "thin"
    }

    withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "11"
        }
    }

    withType<Test> {
        useJUnitPlatform()
    }
}

// injects build infos
springBoot {
    buildInfo {
        properties {
            time = null
        }
    }
}
