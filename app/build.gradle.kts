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
    maven { url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots") } // only needed for SNAPSHOT
}

sourceSets {
    main {
        runtimeClasspath = files(file("src/main/resources"), runtimeClasspath);
    }
}

dependencies {
    implementation("com.uwyn.rife2:rife2:1.3.0")
    runtimeOnly("com.uwyn.rife2:rife2:1.3.0:agent")
    runtimeOnly("org.slf4j:slf4j-simple:2.0.5")
    runtimeOnly("org.eclipse.jetty:jetty-server:11.0.13")
    runtimeOnly("org.eclipse.jetty:jetty-servlet:11.0.13")
    testImplementation("org.jsoup:jsoup:1.15.3")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.0")
}

sourceSets.main {
    resources.exclude("templates/**")
}

tasks {
    register<JavaExec>("precompileHtmlTemplates") {
        classpath = sourceSets["main"].runtimeClasspath
        mainClass.set("rife.template.TemplateDeployer")
        args = listOf(
            "-verbose",
            "-t", "html",
            "-d", "${projectDir}/build/classes/java/main",
            "-encoding", "UTF-8", "${projectDir}/src/main/resources/templates"
        )
    }

    register("precompileTemplates") {
        dependsOn("precompileHtmlTemplates")
    }

    jar {
        dependsOn("precompileTemplates")
    }

    register<Copy>("copyWebapp") {
        from("src/main/")
        include("webapp/**")
        into("$buildDir/webapp")
    }

    register<Jar>("uberJar") {
        dependsOn("jar")
        dependsOn("copyWebapp")
        archiveBaseName.set("rifers-uber")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        manifest {
            attributes["Main-Class"] = "rifers.RifersSiteUber"
        }
        val dependencies = configurations
            .runtimeClasspath.get()
            .map(::zipTree)
        from(dependencies, "$buildDir/webapp")
        with(jar.get())
    }

    register<JavaExec>("run") {
        classpath = sourceSets["main"].runtimeClasspath
        mainClass.set("rifers.RifersSite")
        val rifeAgentJar = configurations.runtimeClasspath.get().files
            .filter { it.toString().contains("rife2") }
            .filter { it.toString().endsWith("-agent.jar") }[0]
        jvmArgs = listOf("-javaagent:$rifeAgentJar")
    }

    named<Test>("test") {
        useJUnitPlatform()
    }
}
