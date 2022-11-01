plugins {
    `java-library`
    application
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
}

sourceSets {
    main {
        runtimeClasspath = files(file("src/main/resources"), runtimeClasspath);
    }
}

application {
    mainClass.set("rifers.RifersSite")
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
    testImplementation("org.jsoup:jsoup:1.15.3")
    runtimeOnly("org.slf4j:slf4j-simple:2.0.3")
    runtimeOnly("org.eclipse.jetty:jetty-server:11.0.12")
    runtimeOnly("org.eclipse.jetty:jetty-servlet:11.0.12")
    implementation("com.uwyn.rife2:rife2:0.5.1")
}

sourceSets.main {
    resources.exclude("templates/**")
}

tasks.register<JavaExec>("precompileHtmlTemplates") {
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("rife.template.TemplateDeployer")
    args = listOf("-verbose",
        "-t", "html",
        "-d", "${projectDir}/build/classes/java/main",
        "-encoding", "UTF-8", "${projectDir}/src/main/resources/templates")
}

tasks.register("precompileTemplates") {
    dependsOn("precompileHtmlTemplates")
}

tasks.jar {
    dependsOn("precompileTemplates")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
