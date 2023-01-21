plugins {
    `java-library`
    java
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

dependencies {
    runtimeOnly("org.slf4j:slf4j-simple:2.0.5")
    runtimeOnly("org.eclipse.jetty:jetty-server:11.0.13")
    runtimeOnly("org.eclipse.jetty:jetty-servlet:11.0.13")
    implementation("com.uwyn.rife2:rife2:0.9.9")
    runtimeOnly("com.uwyn.rife2:rife2:0.9.9:agent")
    testImplementation("org.jsoup:jsoup:1.15.3")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.0")
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

tasks.register<JavaExec>("run") {
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("rifers.RifersSite")
    val rifeAgentJar = configurations.runtimeClasspath.get().files
        .filter { it.toString().contains("rife2") }
        .filter { it.toString().endsWith("-agent.jar") }[0]
    jvmArgs = listOf("-javaagent:$rifeAgentJar")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
