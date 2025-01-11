package com.example;

import rife.bld.Project;
import java.util.List;
import static rife.bld.dependencies.Repository.*;
import static rife.bld.dependencies.Scope.*;

public class MyAppBuild extends Project {
    public MyAppBuild() {
        pkg = "com.example";
        name = "my-app";
        mainClass = "com.example.MyApp";
        version = version(0,1,0);

        downloadSources = true;
        repositories = List.of(MAVEN_CENTRAL, RIFE2_RELEASES);
        scope(test)
            .include(dependency("org.junit.jupiter",
                                "junit-jupiter",
                                version(5,11,4)))
            .include(dependency("org.junit.platform",
                                "junit-platform-console-standalone",
                                version(1,11,4)));
    }

    public static void main(String[] args) {
        new MyAppBuild().start(args);
    }
}