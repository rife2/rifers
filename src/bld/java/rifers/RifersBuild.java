package rifers;

import rife.bld.WebProject;

import java.util.List;

import static rife.bld.dependencies.Repository.*;
import static rife.bld.dependencies.Scope.*;
import static rife.bld.operations.TemplateType.*;

public class RifersBuild extends WebProject {
    public RifersBuild() {
        pkg = "rifers";
        name = "Rifers";
        mainClass = "rifers.RifersSite";
        uberJarMainClass = "rifers.RifersSiteUber";
        version = version(1,0,0);

        downloadSources = true;
        autoDownloadPurge = true;

        repositories = List.of(MAVEN_CENTRAL, RIFE2_RELEASES, RIFE2_SNAPSHOTS);
        scope(compile)
            .include(dependency("com.uwyn.rife2", "rife2", version(1,10,0,"SNAPSHOT")));
        // the instrumentation agent enables web continuations, activated for run and test
        useRife2Agent(version(1,10,0,"SNAPSHOT"));
        scope(test)
            .include(dependency("org.jsoup", "jsoup", version(1,18,3)))
            .include(dependency("org.junit.jupiter", "junit-jupiter", version(5,11,4)))
            .include(dependency("org.junit.platform", "junit-platform-console-standalone", version(1,11,4)))
            .include(dependency("com.h2database", "h2", version(2,3,232)));
        scope(standalone)
            .include(dependency("org.eclipse.jetty.ee10", "jetty-ee10", version(12,0,16)))
            .include(dependency("org.eclipse.jetty.ee10", "jetty-ee10-servlet", version(12,0,16)))
            .include(dependency("org.slf4j", "slf4j-simple", version(2,0,16)))
            // the migrations demo runs real migrations against an in-memory H2
            .include(dependency("com.h2database", "h2", version(2,3,232)))
            // the testing demo drives forms out of container, which RIFE2 parses with jsoup
            .include(dependency("org.jsoup", "jsoup", version(1,18,3)));

        precompileOperation()
            .templateTypes(HTML);
    }

    public static void main(String[] args) {
        new RifersBuild().start(args);
    }
}