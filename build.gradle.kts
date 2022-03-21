import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.6.4"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"

    id("net.mayope.deployplugin") version "0.0.51"

    kotlin("jvm") version "1.6.10"
    kotlin("plugin.spring") version "1.6.10"
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
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

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
        dockerBuild{

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

tasks {
    named<Jar>("jar") {
        enabled = true
        classifier = "thin"
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "11"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// injects build infos
springBoot {
    buildInfo {
        properties {
            time = null
        }
    }
}
