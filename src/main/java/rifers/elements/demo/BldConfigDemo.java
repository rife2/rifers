package rifers.elements.demo;

import rife.engine.Context;
import rife.engine.Element;
import rife.engine.annotations.Parameter;
import rife.tools.StringUtils;

import java.util.ArrayList;
import java.util.Set;

/**
 * Generates a real bld build file from a few chosen options, server-side, so
 * the visitor can see how little code bld needs and how the file changes as
 * the project changes. Nothing is executed, the output is exactly the
 * Build.java you would write by hand, and the RIFE2 web app option matches
 * what {@code bld create-rife2} scaffolds.
 */
public class BldConfigDemo implements Element {
    @Parameter String name = "my-app";
    @Parameter String pkg = "com.example";
    @Parameter String type = "app";

    public void process(Context c) {
        var artifact = name.isBlank() ? "my-app" : name.trim();
        // filter the artifact into a valid, capitalized Java class name,
        // e.g. "my-app" -> "MyApp", "order service" -> "OrderService"
        var baseName = StringUtils.filterAsIdentifier(artifact, true);
        if (baseName == null) {
            baseName = "App";
        }
        var packageName = packageFrom(pkg);

        var web = "web".equals(type);
        var lib = "lib".equals(type);

        var junit = checkbox(c, "junit", true);
        var logging = checkbox(c, "logging", false);
        var commons = checkbox(c, "commons", false);

        var t = c.template("demo/bldconfig");
        t.setValue("run_url", c.urlFor(c.route()).toString());
        t.setValueEncoded("name", artifact);
        t.setValueEncoded("pkg", packageName);
        // the artifact lands inside a Java string literal, so escape it there
        t.setValueEncoded("artifact", StringUtils.encodeString(artifact));
        t.setValueEncoded("package", packageName);
        t.setValueEncoded("baseName", baseName);
        t.setValue("buildClass", baseName + "Build");
        t.setValue("baseClass", web ? "WebProject" : "Project");
        t.setValue("type_" + (web ? "web" : lib ? "lib" : "app"), " selected");
        if (junit) {
            t.setValue("junit_checked", " checked");
        }
        if (logging) {
            t.setValue("logging_checked", " checked");
        }
        if (commons) {
            t.setValue("commons_checked", " checked");
        }

        // the project type drives the base class, the main class and, for the
        // web app, the uber jar main class and template precompilation
        if (web) {
            t.appendBlock("templateTypeImport", "tt_import");
            t.appendBlock("mainClassLine", "main_web");
            t.appendBlock("precompile", "precompile_web");
        } else if (!lib) {
            t.appendBlock("mainClassLine", "main_app");
        }

        // every selected dependency chains into its scope; the web app brings
        // rife2, jsoup and an embedded Jetty of its own
        if (web) {
            t.appendBlock("compile_includes", "inc_rife2");
        }
        if (logging) {
            t.appendBlock("compile_includes", "inc_slf4j_api");
        }
        if (commons) {
            t.appendBlock("compile_includes", "inc_commons");
        }
        if (web || logging || commons) {
            t.appendBlock("deps", "scope_compile");
        }

        if (web) {
            t.appendBlock("test_includes", "inc_jsoup");
        }
        if (junit) {
            t.appendBlock("test_includes", "inc_junit_jupiter");
            t.appendBlock("test_includes", "inc_junit_console");
        }
        if (web || junit) {
            t.appendBlock("deps", "scope_test");
        }

        if (web) {
            t.appendBlock("standalone_includes", "inc_jetty");
            t.appendBlock("standalone_includes", "inc_jetty_servlet");
            t.appendBlock("standalone_includes", "inc_slf4j_simple");
            t.appendBlock("deps", "scope_standalone");
        }

        c.printHtmxFragment(t, "stage");
    }

    /**
     * Reads an optional checkbox. Its hidden companion sends "false" when the
     * box is unchecked, so the last submitted value is the real state, and no
     * value at all (the first load) falls back to the default.
     */
    static boolean checkbox(Context c, String name, boolean def) {
        var values = c.parameterValues(name);
        if (values == null || values.length == 0) {
            return def;
        }
        return "true".equals(values[values.length - 1]);
    }

    // reserved words and literals that cannot stand alone as a package segment
    static final Set<String> JAVA_KEYWORDS = Set.of(
        "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
        "class", "const", "continue", "default", "do", "double", "else", "enum",
        "extends", "final", "finally", "float", "for", "goto", "if", "implements",
        "import", "instanceof", "int", "interface", "long", "native", "new",
        "package", "private", "protected", "public", "return", "short", "static",
        "strictfp", "super", "switch", "synchronized", "this", "throw", "throws",
        "transient", "try", "void", "volatile", "while", "true", "false", "null", "_");

    /**
     * Turns free text into a lower-case dotted package name where every segment
     * is a valid Java identifier, keeping only identifier characters, prefixing
     * an underscore where a segment would start with a digit or collide with a
     * keyword, and falling back to a sensible default.
     */
    static String packageFrom(String input) {
        var segments = new ArrayList<String>();
        var segment = new StringBuilder();
        for (var ch : input.trim().toLowerCase().toCharArray()) {
            if (Character.isLetterOrDigit(ch)) {
                segment.append(ch);
            } else if (segment.length() > 0) {
                segments.add(segment.toString());
                segment.setLength(0);
            }
        }
        if (segment.length() > 0) {
            segments.add(segment.toString());
        }

        var result = new StringBuilder();
        for (var seg : segments) {
            // a segment that starts with a digit or is a keyword is still made
            // valid by prefixing an underscore, e.g. 123 -> _123, class -> _class
            if (Character.isDigit(seg.charAt(0)) || JAVA_KEYWORDS.contains(seg)) {
                seg = "_" + seg;
            }
            if (result.length() > 0) {
                result.append('.');
            }
            result.append(seg);
        }
        return result.isEmpty() ? "com.example" : result.toString();
    }
}
