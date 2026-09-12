package rifers;

import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import rife.test.MockConversation;
import rife.test.MockRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

// what the build produces and what the pages claim it ships
public class BuildTest extends SiteTestSupport {
    @Test
    void verifyTheDeployedArchivesNeedNothingAddedToRun() throws Exception {
        // out of container everything resolves from the source tree and the agent is
        // applied for us, so none of these gaps can fail a test, they only surface once
        // the war runs in a container, which is why they are asserted on the build
        var build = Files.readString(Path.of("src/bld/java/rifers/RifersBuild.java"));
        var types = build.replaceAll("(?s).*\\.templateTypes\\(([^)]*)\\).*", "$1");
        assertNotEquals(build, types, "the build has to declare the precompiled template types");
        try (var templates = Files.walk(Path.of("src/main/resources/templates"))) {
            templates.filter(Files::isRegularFile)
                .map(f -> f.getFileName().toString())
                .filter(f -> f.lastIndexOf('.') > 0)
                .map(f -> f.substring(f.lastIndexOf('.') + 1).toUpperCase())
                .filter(t -> !"DS_STORE".equals(t))
                .distinct()
                .forEach(t -> assertTrue(types.contains(t),
                    "templates/*." + t.toLowerCase() + " exists, so " + t + " has to be precompiled, got: " + types));
        }
        // the continuations demos throw ContinuationsNotActiveException without this
        for (var command : new String[]{"war", "uberjar"}) {
            assertTrue(build.replaceAll("(?s).*public void " + command + "\\(\\)[^{]*\\{", "")
                    .replaceAll("(?s)\\}.*", "").contains("instrument()"),
                command + "() has to instrument before packaging");
        }
        // the embedded server turns async on by itself, a container never does
        var descriptor = Files.readString(Path.of(WEBAPP, "WEB-INF/web.xml"));
        assertTrue(descriptor.contains("<async-supported>true</async-supported>"),
            "the RIFE2 filter has to declare async support or SSE fails in a container");
        // test and standalone scope both keep these demos green here, only a packaged
        // scope also gets them onto the container's classpath
        var packaged = build.replaceAll("(?s).*scope\\(runtime\\)", "")
            .replaceAll("(?s)\\bscope\\((?!runtime).*", "");
        for (var artifact : new String[]{"h2", "jsoup"}) {
            assertTrue(packaged.contains("\"" + artifact + "\""),
                artifact + " is loaded at run time, so it has to be in a scope the war packages");
        }
    }

    @Test
    void verifyNoPageNamesAJUnitVersionItDoesNotShip() throws Exception {
        // "JUnit 5" outlived junit-jupiter 5.x on both pages and nothing noticed, so
        // any major named in prose now has to match the dependency underneath it
        var config = Files.readString(Path.of("src/main/resources/templates/demo/bldconfig.html"));
        var shipped = Pattern.compile("junit-jupiter\",\\s*\n?\\s*version\\((\\d+)").matcher(config);
        assertTrue(shipped.find(), "the configurator has to pin a junit-jupiter version");
        var major = shipped.group(1);
        try (var templates = Files.walk(Path.of("src/main/resources/templates"))) {
            for (var template : templates.filter(f -> f.toString().endsWith(".html")).toList()) {
                var named = Pattern.compile("JUnit\\s+(\\d+)").matcher(Files.readString(template));
                while (named.find()) {
                    assertEquals(major, named.group(1),
                        template.getFileName() + " names JUnit " + named.group(1) + " but ships junit-jupiter " + major);
                }
            }
        }
    }

    @Test
    void verifyBldConfigPinsTheShippingRife2Version() {
        // the configurator shows what bld create-rife2 scaffolds, so its pinned rife2
        // has to be the one this site ships against; on a snapshot the pin trails it
        // and points at the last release instead
        var running = rife.Version.getVersion();
        if (running.contains("-")) {
            return;
        }
        var expected = "version(" + running.replace('.', ',') + ")";
        var webCode = new MockConversation(new RifersSite())
            .doRequest("/demo/bld-config", new MockRequest().parameter("type", "web"))
            .getParsedHtml().getDocument().selectFirst(".bld-config-out").text();
        assertTrue(webCode.contains(expected),
            "expected the configurator to pin rife2 " + expected + ", got: " + webCode);
    }
}
