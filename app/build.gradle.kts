import com.uwyn.rife2.gradle.TemplateType.*

plugins {
    application
    id("com.uwyn.rife2") version "1.0.3"
}

base {
    archivesName.set("rifers")
    version = 1.0
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots") } // only needed for SNAPSHOT
}

rife2 {
    version.set("1.4.0")
    useAgent.set(true)
    precompiledTemplateTypes.add(HTML)
    uberMainClass.set("rifers.RifersSiteUber")
}

dependencies {
    testImplementation("org.jsoup:jsoup:1.15.3")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.0")
}

application {
    mainClass.set("rifers.RifersSite")
}

tasks {
    named<Test>("test") {
        useJUnitPlatform()
    }
}
