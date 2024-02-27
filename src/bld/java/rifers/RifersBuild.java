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

        repositories = List.of(MAVEN_CENTRAL, RIFE2_RELEASES);
        scope(compile)
            .include(dependency("com.uwyn.rife2", "rife2", version(1,7,3)));
        scope(test)
            .include(dependency("org.jsoup", "jsoup", version(1,17,2)))
            .include(dependency("org.junit.jupiter", "junit-jupiter", version(5,10,2)))
            .include(dependency("org.junit.platform", "junit-platform-console-standalone", version(1,10,2)));
        scope(standalone)
            .include(dependency("org.eclipse.jetty.ee10", "jetty-ee10", version(12,0,6)))
            .include(dependency("org.eclipse.jetty.ee10", "jetty-ee10-servlet", version(12,0,6)))
            .include(dependency("org.slf4j", "slf4j-simple", version(2,0,12)));

        precompileOperation()
            .templateTypes(HTML);
    }

    public static void main(String[] args) {
        new RifersBuild().start(args);
    }
}