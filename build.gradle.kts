plugins {
    `java-library`
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("se.patrikerdes.use-latest-versions") version "0.2.18"
    id("com.github.ben-manes.versions") version "0.50.0"
}

dependencies {
    // Use implementation instead of compileOnly for jadx-core
    implementation("io.github.skylot:jadx-core:1.5.1-SNAPSHOT") {
        isChanging = true
    }

    // HTTP and JSON deps
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.0")
    
    // Logging
    implementation("org.slf4j:slf4j-api:1.7.32")
    implementation("ch.qos.logback:logback-classic:1.4.14")

    // Markdown parser
    implementation("com.vladsch.flexmark:flexmark:0.64.8")
    implementation("com.vladsch.flexmark:flexmark-util:0.64.8")
    implementation("com.vladsch.flexmark:flexmark-ext-tables:0.64.8")
    
    // Syntax highlighting
    implementation("com.fifesoft:rsyntaxtextarea:3.3.4")

    // Testing
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.1")
    testImplementation("io.github.skylot:jadx-smali-input:1.5.1-SNAPSHOT") {
        isChanging = true
    }
}

repositories {
    mavenCentral()
    maven(url = "https://s01.oss.sonatype.org/content/repositories/snapshots/")
    google()
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

version = System.getenv("VERSION") ?: "dev"

tasks {
    withType<Test> {
        useJUnitPlatform()
    }

    shadowJar {
        // Include all dependencies
        archiveClassifier.set("")
        mergeServiceFiles()
        configurations = listOf(project.configurations.runtimeClasspath.get())
    }

    // Make shadowJar the default jar task
    named<Jar>("jar") {
        enabled = false
    }

    build {
        dependsOn(shadowJar)
    }

    register<Copy>("dist") {
        dependsOn(shadowJar)
        from(shadowJar)
        into(layout.buildDirectory.dir("dist"))
    }
}