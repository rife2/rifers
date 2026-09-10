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
        version = version(2,0,1);

        downloadSources = true;
        autoDownloadPurge = true;

        repositories = List.of(MAVEN_CENTRAL, RIFE2_RELEASES);
        scope(compile)
            .include(dependency("com.uwyn.rife2", "rife2", version(1,10,0)));
        // the instrumentation agent enables web continuations, activated for run and test
        useRife2Agent(version(1,10,0));
        scope(test)
            .include(dependency("org.jsoup", "jsoup", version(1,23,2)))
            .include(dependency("org.junit.jupiter", "junit-jupiter", version(6,1,3)))
            .include(dependency("org.junit.platform", "junit-platform-console-standalone", version(6,1,3)))
            .include(dependency("com.h2database", "h2", version(2,5,250)));
        scope(standalone)
            .include(dependency("org.eclipse.jetty.ee10", "jetty-ee10", version(12,1,13)))
            .include(dependency("org.eclipse.jetty.ee10", "jetty-ee10-servlet", version(12,1,13)))
            .include(dependency("org.slf4j", "slf4j-simple", version(2,0,19)))
            // the migrations demo runs real migrations against an in-memory H2
            .include(dependency("com.h2database", "h2", version(2,5,250)))
            // the testing demo drives forms out of container, which RIFE2 parses with jsoup
            .include(dependency("org.jsoup", "jsoup", version(1,23,2)));

        // the archives ship the precompiled template classes, not the template
        // sources, so a type left out here only goes missing once deployed
        precompileOperation()
            .templateTypes(HTML, TXT);
    }

    // the deployed archives carry no agent, so instrument the continuations
    // ahead of time instead of relying on the container's JVM to do it
    @Override
    public void war() throws Exception {
        instrument();
        super.war();
    }

    @Override
    public void uberjar() throws Exception {
        instrument();
        super.uberjar();
    }

    public static void main(String[] args) {
        new RifersBuild().start(args);
    }
}