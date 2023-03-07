plugins {
    war
}

version = 1.0
group = "com.uwyn"

base {
    archivesName.set("rifers")
}

repositories {
    mavenCentral()
    maven { url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots") } // only needed for SNAPSHOT
}

dependencies {
    implementation(project(":app"))
}

tasks.war {
    webAppDirectory.set(file("../app/src/main/webapp"))
    webXml = file("src/web.xml")
}

tasks.test {
    enabled = false
}