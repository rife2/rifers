package com.example;

import rife.bld.Project;
import java.util.List;
import static rife.bld.dependencies.Repository.*;
import static rife.bld.dependencies.Scope.*;

public class MyappBuild extends Project {
    public MyappBuild() {
        pkg = "com.example";
        name = "Myapp";
        mainClass = "com.example.MyappMain";
        version = version(0,1,0);

        downloadSources = true;
        repositories = List.of(MAVEN_CENTRAL, RIFE2_RELEASES);
        scope(test)
            .include(dependency("org.junit.jupiter",
                                "junit-jupiter",
                                version(5,10,0)))
            .include(dependency("org.junit.platform",
                                "junit-platform-console-standalone",
                                version(1,10,0)));
    }

    public static void main(String[] args) {
        new MyappBuild().start(args);
    }
}