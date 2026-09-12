package rifers;

import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import rife.test.MockConversation;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

// the pages themselves: content, navigation, assets and link previews
public class SiteTest extends SiteTestSupport {
    @Test
    void verifyHomePage() {
        var m = new MockConversation(new RifersSite());
        assertEquals("RIFE2 - Full-stack, no-declaration web framework for modern Java",
            m.doRequest("/").getParsedHtml().getTitle());

        var doc = page("/");
        assertTrue(doc.select("h1.pitch").text().contains("full-stack framework"));
        assertFalse(doc.select("meta[name=description]").attr("content").isEmpty());
        assertFalse(doc.select("link[rel=canonical]").attr("href").isEmpty());
    }

    @Test
    void verifyBldPage() {
        var m = new MockConversation(new RifersSite());
        assertEquals("bld - pure Java build tool",
            m.doRequest("/bld").getParsedHtml().getTitle());

        var doc = page("/bld");
        // the hero offers a zero-install one-liner per platform, the shell one is
        // shown by default and the JS switch reveals the PowerShell panel
        assertEquals("bash -c \"$(curl -fsSL https://rife2.com/bld/create.sh)\"",
            doc.select("#os-unix .install code").text());
        assertEquals("irm https://rife2.com/bld/create.ps1 | iex",
            doc.select("#os-windows .install code").text());
        var headings = doc.select("h2").eachText();
        assertTrue(headings.contains("One jar, everything included"));
        assertTrue(headings.contains("Ready for AI agents"));
        assertTrue(headings.contains("Get started"));
    }

    @Test
    void verifyLlmsTxt() {
        // /llms.txt is served by a route (with .txt dropped from the pass-through
        // suffixes), a curated map of the framework for AI agents
        var m = new MockConversation(new RifersSite());
        var response = m.doRequest("/llms.txt");
        assertEquals(200, response.getStatus());
        assertTrue(response.getContentType().contains("text/plain"), "llms.txt is served as plain text");
        var body = response.getText();
        assertTrue(body.startsWith("# RIFE2"), "llms.txt starts with the project heading");
        assertTrue(body.contains("https://github.com/rife2/rife2/wiki/Query-Builders"), "llms.txt links into the wiki");
        // the home page has the AI section and points agents at the llms.txt
        var doc = page("/");
        assertTrue(doc.select("h2").eachText().contains("Built for AI-assisted development"), "home has the AI section");
        assertEquals(1, doc.select("a[href$='llms.txt']").size(), "home links to llms.txt");
    }

    @Test
    void verifyNavigation() {
        for (var path : new String[]{"/", "/bld"}) {
            var doc = page(path);
            var nav = doc.select("header .nav a");
            assertEquals(4, nav.size(), "expected 4 nav links on " + path);
            assertEquals(1, doc.select("header .nav a.active").size(), "expected one active nav link on " + path);
        }
        assertEquals("RIFE2", page("/").select("header .nav a.active").text());
        assertEquals("bld", page("/bld").select("header .nav a.active").text());
    }

    @Test
    void verifyInternalLinksResolve() {
        var m = new MockConversation(new RifersSite());
        for (var path : new String[]{"/", "/bld"}) {
            var doc = page(path);
            for (var link : doc.select("a[href]")) {
                var href = link.attr("href");
                if (href.startsWith("/") && !href.startsWith("//")) {
                    assertEquals(200, m.doRequest(href).getStatus(), "broken internal link " + href + " on " + path);
                }
            }
        }
    }

    @Test
    void verifyExampleFilesExist() {
        var doc = page("/");
        var examples = doc.select("pre[data-src]");
        assertEquals(15, examples.size());
        for (var example : examples) {
            var src = example.attr("data-src");
            var file = src.substring(src.indexOf("examples/"));
            assertTrue(Files.exists(Path.of(WEBAPP, file)), "missing example file " + file);
        }
    }

    @Test
    void verifyExampleTabs() {
        // both pages carry more than one tab group now (the examples and the
        // per-OS hero switch), so check the invariants within each group: its
        // tabs and panels share a container, and exactly one of each is active
        for (var path : new String[]{"/", "/bld"}) {
            var doc = page(path);
            var tablists = doc.select("[role=tablist]");
            assertFalse(tablists.isEmpty(), "no tab group on " + path);
            for (var tablist : tablists) {
                var scope = tablist.parent();
                var tabs = scope.select("[role=tab]");
                var panels = scope.select("[role=tabpanel]");
                assertEquals(tabs.size(), panels.size(), "tabs and panels differ on " + path);
                assertEquals(1, scope.select("[role=tab][aria-selected=true]").size(),
                    "exactly one selected tab on " + path);
                assertEquals(1, panels.size() - scope.select("[role=tabpanel][hidden]").size(),
                    "exactly one visible panel on " + path);
            }
        }
    }

    @Test
    void verifyAssetsExist() {
        for (var path : new String[]{"/", "/bld"}) {
            var doc = page(path);
            for (var sheet : doc.select("link[rel=stylesheet]")) {
                var href = sheet.attr("href");
                if (href.contains("css/")) {
                    var file = href.substring(href.indexOf("css/"), href.indexOf('?') == -1 ? href.length() : href.indexOf('?'));
                    assertTrue(Files.exists(Path.of(WEBAPP, file)), "missing stylesheet " + file + " on " + path);
                }
            }
            for (var script : doc.select("script[src]")) {
                var src = script.attr("src");
                // drop the ?rnd= cache-busting query before resolving the file
                var file = src.substring(src.indexOf("js/"), src.indexOf('?') == -1 ? src.length() : src.indexOf('?'));
                assertTrue(Files.exists(Path.of(WEBAPP, file)), "missing script " + file + " on " + path);
            }
        }
    }

    @Test
    void verifyBldComparison() {
        var doc = page("/bld");
        var examples = doc.selectFirst(".examples");
        assertNotNull(examples, "the bld page should have a comparison tab set");
        var tabs = examples.select("[role=tab]").eachText();
        assertEquals(java.util.List.of("bld", "Maven", "Gradle"), tabs, "three build tools compared");
        // every comparison panel loads a real example file that must exist
        for (var pre : examples.select("pre[data-src]")) {
            var src = pre.attr("data-src");
            var file = src.substring(src.indexOf("examples/"));
            assertTrue(Files.exists(Path.of(WEBAPP, file)), "missing comparison example file " + file);
        }
    }

    @Test
    void verifyOgImagesMatchTheirDeclaredSize() throws Exception {
        // the bld card was 1200x632 against a declared 630 through three revisions and
        // nothing noticed, because a wrong size only shows up once a platform crops it
        var common = Files.readString(Path.of("src/main/resources/templates/common.html"));
        var declared = new int[2];
        var dimension = Pattern.compile("og:image:(width|height)\" content=\"(\\d+)").matcher(common);
        while (dimension.find()) {
            declared["width".equals(dimension.group(1)) ? 0 : 1] = Integer.parseInt(dimension.group(2));
        }
        assertTrue(declared[0] > 0 && declared[1] > 0, "the page has to declare an og:image size");
        var referenced = 0;
        try (var elements = Files.walk(Path.of("src/main/java/rifers/elements"))) {
            for (var element : elements.filter(f -> f.toString().endsWith(".java")).toList()) {
                var image = Pattern.compile("\"og_image\",\\s*\"([^\"]+)\"").matcher(Files.readString(element));
                while (image.find()) {
                    var file = Path.of(WEBAPP, "images", image.group(1));
                    assertTrue(Files.exists(file), "missing og image " + image.group(1));
                    var png = Files.readAllBytes(file);
                    // the IHDR chunk carries the real size, no image library needed
                    var width = ByteBuffer.wrap(png, 16, 4).getInt();
                    var height = ByteBuffer.wrap(png, 20, 4).getInt();
                    assertEquals(declared[0] + "x" + declared[1], width + "x" + height,
                        image.group(1) + " does not match the declared og:image size");
                    referenced++;
                }
            }
        }
        assertTrue(referenced >= 2, "both pages have to declare an og image, found " + referenced);
    }

    @Test
    void verifyActiveNavIndicatesCurrentPage() {
        var m = new MockConversation(new RifersSite());
        var homeActive = m.doRequest("/").getParsedHtml().getDocument().select("nav.nav a[aria-current=page]");
        assertEquals(1, homeActive.size(), "exactly one nav link marks the current page");
        assertEquals("RIFE2", homeActive.first().text(), "the RIFE2 link is current on the home page");
        assertTrue(homeActive.first().hasClass("active"), "the current link keeps its active class");
        var bldActive = m.doRequest("/bld").getParsedHtml().getDocument().select("nav.nav a[aria-current=page]");
        assertEquals(1, bldActive.size(), "exactly one nav link marks the current page on the bld page");
        assertEquals("bld", bldActive.first().text(), "the bld link is current on the bld page");
    }

    @Test
    void verifyFooter() {
        for (var path : new String[]{"/", "/bld"}) {
            var doc = page(path);
            assertEquals(3, doc.select(".site-footer .footer-links > div").size(), "expected 3 footer columns on " + path);
            var homeLinks = doc.select(".footer-links a").stream()
                .filter(a -> a.text().equals("Home"))
                .map(a -> a.attr("href"))
                .toList();
            assertEquals(2, homeLinks.size(), "expected RIFE2 and bld Home links in footer on " + path);
            var source = doc.selectFirst(".site-footer .site-source a");
            assertNotNull(source, "expected a link to the site's own source on " + path);
            assertEquals("https://github.com/rife2/rifers", source.attr("href"), "source link points at the rifers repo on " + path);
        }
    }
}
