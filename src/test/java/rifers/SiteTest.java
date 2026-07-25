package rifers;

import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import rife.json.Json;
import rife.json.JsonObject;
import rife.test.MockConversation;
import rife.test.MockRequest;
import rife.tools.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static rife.engine.RequestMethod.POST;

public class SiteTest {
    static final String WEBAPP = "src/main/webapp";

    Document page(String path) {
        var m = new MockConversation(new RifersSite());
        var response = m.doRequest(path);
        assertEquals(200, response.getStatus(), "expected " + path + " to render");
        return response.getParsedHtml().getDocument();
    }

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
    void verifyDemoPagesLoadPrismTheme() {
        // opened in its own tab a demo brings its own head, and highlighting needs
        // the theme as well as the highlighter: with prism.js but no prism.css the
        // tokens silently fall back to flat text instead of failing outright
        for (var path : new String[]{"/demo/hello", "/demo/link", "/demo/form", "/demo/counter",
            "/demo/validation", "/demo/csrf", "/demo/templates", "/demo/htmx", "/demo/sse",
            "/demo/workflow", "/demo/migrations", "/demo/queries", "/demo/gqm", "/demo/test",
            "/demo/bld-config"}) {
            var doc = page(path);
            if (!doc.select("script[src*=js/prism.js]").isEmpty()) {
                assertFalse(doc.select("link[href*=css/prism.css]").isEmpty(),
                    "highlights code with prism.js but never loads prism.css on " + path);
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
    void verifyJsonDemo() {
        var m = new MockConversation(new RifersSite());
        // the endpoint is a real JSON API: application/json, straight from a record
        var response = m.doRequest("/demo/json");
        assertEquals(200, response.getStatus());
        assertTrue(response.getContentType().contains("application/json"), "expected a JSON content type");
        var json = (JsonObject) Json.parse(response.getText());
        assertEquals("world", json.getString("name"), "the record field becomes the JSON key");
        assertEquals("Hello world!", json.getString("message"));
        // the name flows into the JSON so the live demo changes as you type
        var custom = (JsonObject) Json.parse(m.doRequest("/demo/json",
            new MockRequest().parameter("name", "Ada")).getText());
        assertEquals("Ada", custom.getString("name"));
        assertEquals("Hello Ada!", custom.getString("message"));
    }

    @Test
    void verifyFormDemo() {
        var m = new MockConversation(new RifersSite());

        var form = page("/demo/form");
        assertEquals(1, form.select("form input[name=name]").size(), "GET should render the form");

        var greeting = new MockConversation(new RifersSite())
            .doRequest("/demo/form", new MockRequest().method(POST).parameter("name", "Geert"));
        assertEquals(200, greeting.getStatus());
        assertTrue(greeting.getText().contains("Hello Geert!"), "POST should echo the name");
    }

    @Test
    void verifyFormDemoEncodesInput() {
        var response = new MockConversation(new RifersSite())
            .doRequest("/demo/form", new MockRequest().method(POST).parameter("name", "<script>alert(1)</script>"));
        var body = response.getText();
        assertFalse(body.contains("<script>alert(1)</script>"), "raw HTML input must not appear unencoded");
        assertTrue(body.contains("&lt;script&gt;"), "input should be HTML-encoded");
    }

    @Test
    void verifyTemplatesDemo() {
        var m = new MockConversation(new RifersSite());
        // welcome: the same logic-less template renders the welcome page with no tags
        var welcome = m.doRequest("/demo/templates").getParsedHtml().getDocument();
        assertTrue(welcome.select(".demo-tpl-render").text().contains("Welcome"), "welcome state renders the welcome block");
        assertEquals(0, welcome.select(".demo-tpl-render .demo-tpl-tag").size(), "no tags in the welcome state");
        // dashboard: the same template renders the dashboard, with a tag list built by appendBlock in a loop
        var dashboard = m.doRequest("/demo/templates", new MockRequest()
            .parameter("state", "dashboard").parameter("tags", "java, rife2, bld")).getParsedHtml().getDocument();
        var rendered = dashboard.select(".demo-tpl-render");
        assertTrue(rendered.text().contains("Hi Ada"), "dashboard state renders the dashboard block");
        assertEquals(3, rendered.select(".demo-tpl-tag").size(), "each tag becomes one appended block");
    }

    @Test
    void verifyValidationDemo() {
        var m = new MockConversation(new RifersSite());
        // empty fields are reported as required, and a missing age is required (not out of range)
        var empty = m.doRequest("/demo/validation", new MockRequest().method(POST)
            .parameter("name", "").parameter("email", "nope").parameter("age", "")).getParsedHtml().getDocument();
        var emptyErrors = empty.select(".demo-verror").eachText();
        assertEquals(0, empty.select(".result-pass").size(), "an invalid submission is not valid");
        assertTrue(emptyErrors.contains("Name is required"), "empty name is required");
        assertTrue(emptyErrors.contains("Email must be a valid address"), "bad email is invalid");
        assertTrue(emptyErrors.contains("Age is required"), "a missing age is required, not out of range");
        // an out-of-range age is reported differently from a missing one
        var badAge = m.doRequest("/demo/validation", new MockRequest().method(POST)
            .parameter("name", "Ada").parameter("email", "ada@example.com").parameter("age", "5")).getParsedHtml().getDocument();
        assertTrue(badAge.select(".demo-verror").eachText().contains("Age must be a whole number from 18 to 120"), "an out-of-range age is invalid");
        // validating only the account group ignores the profile (age) field entirely
        var accountOnly = m.doRequest("/demo/validation", new MockRequest().method(POST)
            .parameter("group", "account").parameter("name", "Ada").parameter("email", "ada@example.com").parameter("age", ""))
            .getParsedHtml().getDocument();
        assertEquals(1, accountOnly.select(".result-pass").size(), "the account group passes even with no age");
        // a fully valid submission passes
        var valid = m.doRequest("/demo/validation", new MockRequest().method(POST)
            .parameter("name", "Ada").parameter("email", "ada@example.com").parameter("age", "36")).getParsedHtml().getDocument();
        assertEquals(1, valid.select(".result-pass").size(), "a valid submission passes");
        assertEquals(0, valid.select(".demo-verror").size(), "no field errors when valid");
    }

    @Test
    void verifySseDemo() {
        var doc = page("/demo/sse");
        // the stage connects to the stream route (route:sseStream) once
        var stage = doc.select(".demo-stage[sse-connect]").first();
        assertNotNull(stage, "clock page should have an SSE-connected stage");
        assertTrue(stage.attr("sse-connect").endsWith("/demo/sse/stream"), "sse-connect should resolve to the stream route");
        // the clock and the shared tap tally are separate swap targets on the one connection
        assertEquals(1, stage.select("[sse-swap=clock]").size(), "a clock swap target");
        assertEquals(1, stage.select("[sse-swap=taps]").size(), "a taps swap target");
        assertEquals(1, doc.select(".demo-clock").size(), "clock row should render");
        assertTrue(doc.select(".demo-clock").text().matches("\\d\\d:\\d\\d:\\d\\d"), "clock should show a time");
        assertTrue(stage.select("button[hx-post]").attr("hx-post").endsWith("/demo/sse/tap"), "tap button posts to the tap route");
    }

    @Test
    void verifyTapBroadcast() {
        var m = new MockConversation(new RifersSite());
        // the tally is shared server-wide, so each tap increments what the next render shows
        var before = tapCount(m);
        assertEquals(200, m.doRequest("/demo/sse/tap", new MockRequest().method(POST)).getStatus());
        assertEquals(before + 1, tapCount(m), "a tap increments the shared tally");
    }

    private static int tapCount(MockConversation m) {
        var doc = m.doRequest("/demo/sse").getParsedHtml().getDocument();
        return Integer.parseInt(doc.select(".demo-tap-count").text().trim());
    }

    @Test
    void verifyInlineDemos() {
        var doc = page("/");
        var inline = doc.select("details.demo-inline");
        assertEquals(15, inline.size(), "expected fifteen inline 'Try it live' demos");

        for (var demo : inline) {
            assertEquals(1, demo.select("summary.demo-toggle").size(), "each demo needs a summary toggle");
            var api = demo.select(".demo-json-live").first();
            if (api != null) {
                // the JSON demo calls the real API from the browser, no HTML swap
                var endpoint = api.attr("data-endpoint");
                assertFalse(endpoint.contains("{{"), "endpoint route tag should resolve: " + endpoint);
                assertTrue(endpoint.endsWith("/demo/json"), "json demo calls the /demo/json API: " + endpoint);
                continue;
            }
            var loader = demo.select("[hx-get]").first();
            assertNotNull(loader, "each demo lazily loads its live version over htmx");
            var url = loader.attr("hx-get");
            assertFalse(url.contains("{{"), "hx-get route tag should resolve: " + url);
            assertTrue(loader.attr("hx-trigger").contains("toggle"), "demo should load when the details opens");
        }

        // the inline demos reuse the standalone demo pages / endpoints as their single source
        var urls = doc.select("details.demo-inline [hx-get]").eachAttr("hx-get");
        assertTrue(urls.stream().anyMatch(u -> u.endsWith("/demo/form")), "form demo reuses /demo/form");
        assertTrue(urls.stream().anyMatch(u -> u.endsWith("/demo/sse")), "sse demo reuses /demo/sse");
        assertTrue(urls.stream().anyMatch(u -> u.endsWith("/demo/counter")), "continuations demo reuses /demo/counter");
        assertTrue(urls.stream().anyMatch(u -> u.endsWith("/demo/templates")), "templates demo reuses /demo/templates");
        assertTrue(urls.stream().anyMatch(u -> u.endsWith("/demo/validation")), "validation demo reuses /demo/validation");
        assertTrue(urls.stream().anyMatch(u -> u.endsWith("/demo/hello")), "hello demo reuses /demo/hello");
        assertTrue(urls.stream().anyMatch(u -> u.endsWith("/demo/link")), "type-safe links demo reuses /demo/link");
        assertTrue(urls.stream().anyMatch(u -> u.endsWith("/demo/migrations")), "migrations demo reuses /demo/migrations");
        assertTrue(urls.stream().anyMatch(u -> u.endsWith("/demo/queries")), "query builders demo reuses /demo/queries");
        assertTrue(urls.stream().anyMatch(u -> u.endsWith("/demo/workflow")), "workflow demo reuses /demo/workflow");
        assertTrue(urls.stream().anyMatch(u -> u.endsWith("/demo/htmx")), "htmx demo reuses /demo/htmx");
        assertTrue(urls.stream().anyMatch(u -> u.endsWith("/demo/csrf")), "csrf demo reuses /demo/csrf");
        assertTrue(urls.stream().anyMatch(u -> u.endsWith("/demo/gqm")), "query manager demo reuses /demo/gqm");
        assertTrue(urls.stream().anyMatch(u -> u.endsWith("/demo/test")), "testing demo reuses /demo/test");
    }

    @Test
    void verifyHtmxDemo() {
        var m = new MockConversation(new RifersSite());
        // a normal request serves the whole page: the search box plus the full list
        var full = m.doRequest("/demo/htmx").getText();
        assertTrue(full.contains("<html"), "a browser gets the full page");
        assertTrue(full.contains("demo-htmx-search"), "with the search box");
        assertTrue(full.contains("Java") && full.contains("Elixir"), "and the whole list");

        // an htmx keystroke sends back only the filtered list block, no page, no box
        var filteredResponse = m.doRequest("/demo/htmx",
            new MockRequest().htmx().parameter("q", "ru"));
        // it flows through printHtmxFragment, which varies on both htmx headers
        var filteredVary = filteredResponse.getHeaders("Vary");
        assertTrue(filteredVary.contains("HX-Request") && filteredVary.contains("HX-History-Restore-Request"),
            "the fragment varies on both htmx headers: " + filteredVary);
        var filtered = filteredResponse.getText();
        assertFalse(filtered.contains("<html"), "no surrounding document");
        assertFalse(filtered.contains("demo-htmx-search"), "the search box is not re-sent");
        assertTrue(filtered.contains("Rust") && filtered.contains("Ruby"), "matches come back");
        assertFalse(filtered.contains("Java"), "non-matches are filtered out");

        // an htmx history restore carrying a query still gets the whole page, not
        // a bare list, so the browser can rebuild the page from its history
        var restored = m.doRequest("/demo/htmx", new MockRequest()
            .htmx().header("HX-History-Restore-Request", "true").parameter("q", "ru")).getText();
        assertTrue(restored.contains("<html"), "a history restore rebuilds the full page");
        assertTrue(restored.contains("demo-htmx-search"), "including the search box");

        // the initial htmx load (no query) still gets the whole stage, box and all
        var stage = m.doRequest("/demo/htmx", new MockRequest().htmx()).getText();
        assertTrue(stage.contains("demo-htmx-search"), "the stage includes the search box");
        assertFalse(stage.contains("<html"), "but not the surrounding document");

        // an empty result set shows a message
        assertTrue(m.doRequest("/demo/htmx", new MockRequest().htmx().parameter("q", "zzzz"))
            .getText().contains("no matches"), "no matches shows a message");
    }

    @Test
    void verifyTestDemo() {
        var m = new MockConversation(new RifersSite());
        // nothing run yet: the form shows with its expected value, but no result
        var initial = m.doRequest("/demo/test").getParsedHtml().getDocument();
        assertEquals(0, initial.select(".demo-test").size(), "no result until the test is run");
        // the default expectation matches the response, so the out-of-container assertion passes
        var pass = m.doRequest("/demo/test",
            new MockRequest().parameter("expected", "Hello Ada!")).getParsedHtml().getDocument();
        assertEquals(1, pass.select(".result-pass").size(), "default assertion should pass");
        assertTrue(pass.select(".demo-test").text().contains("Hello Ada!"), "should show the submitted form's reply");
        // a wrong expectation makes the same test fail
        var fail = m.doRequest("/demo/test",
            new MockRequest().parameter("expected", "not this")).getParsedHtml().getDocument();
        assertEquals(1, fail.select(".result-fail").size(), "a wrong expectation should fail");
        assertTrue(fail.select(".demo-test").text().contains("not this"), "should show the expected value that failed");
    }

    @Test
    void verifyCsrfDemo() {
        var m = new MockConversation(new RifersSite());
        // nothing submitted yet: the scenario and buttons show, but no outcome
        var initial = m.doRequest("/demo/csrf").getParsedHtml().getDocument();
        assertEquals(0, initial.select(".demo-test").size(), "no result until a button is pressed");
        // the legitimate submit carries the real token, so the transfer runs
        var ok = m.doRequest("/demo/csrf",
            new MockRequest().parameter("forge", "false")).getParsedHtml().getDocument();
        assertEquals(1, ok.select(".result-pass").size(), "a legitimate request should be accepted");
        assertTrue(ok.select(".demo-test").text().contains("transferred $100"), "the protected route should run");
        // a forged cross-site request has no valid token, so it is refused
        var forged = m.doRequest("/demo/csrf",
            new MockRequest().parameter("forge", "true")).getParsedHtml().getDocument();
        assertEquals(1, forged.select(".result-fail").size(), "a forged request should be refused");
        assertTrue(forged.select(".demo-test-badge").text().contains("403"), "the forgery should get 403");
    }

    @Test
    void verifyQueriesDemo() {
        var m = new MockConversation(new RifersSite());
        // no toggles: a bare select, and the shown Java has no where clause
        var base = m.doRequest("/demo/queries").getParsedHtml().getDocument();
        assertEquals("SELECT title, author FROM book", base.select(".demo-query-sql").text().trim(),
            "with nothing toggled the query is just the base select");
        assertFalse(base.select("code.language-java").text().contains(".where("), "no where clause when nothing is toggled");
        assertEquals(0, base.select(".demo-query-note").size(), "PostgreSQL supports every clause");
        // toggling adds the clauses to the shown Java, and the same Java renders
        // differently per database: PostgreSQL writes the boolean as true
        var pg = m.doRequest("/demo/queries", new MockRequest()
            .parameter("stock", "on").parameter("limit", "on").parameter("offset", "on")).getParsedHtml().getDocument();
        var code = pg.select("code.language-java").text();
        assertTrue(code.contains(".where(\"in_stock\", \"=\", true)") && code.contains(".limit(5)")
            && code.contains(".offset(10)"), "the shown Java reflects the toggled clauses");
        var sql = pg.select(".demo-query-sql").text();
        assertTrue(sql.contains("in_stock = true"), "PostgreSQL writes the boolean as true");
        assertTrue(sql.contains("LIMIT 5 OFFSET 10"), "PostgreSQL uses trailing LIMIT and OFFSET");
        // MySQL writes the very same boolean as 1
        var mysql = m.doRequest("/demo/queries", new MockRequest()
            .parameter("stock", "on").parameter("db", "mysql")).getParsedHtml().getDocument();
        assertTrue(mysql.select(".demo-query-sql").text().contains("in_stock = 1"), "MySQL writes the boolean as 1");
        // HSQLDB puts LIMIT right after SELECT
        var hsql = m.doRequest("/demo/queries", new MockRequest()
            .parameter("limit", "on").parameter("db", "hsqldb")).getParsedHtml().getDocument();
        assertTrue(hsql.select(".demo-query-sql").text().contains("SELECT LIMIT"), "HSQLDB places LIMIT after SELECT");
        // Oracle has no LIMIT keyword, so RIFE2 drops the clause and explains why
        var oracle = m.doRequest("/demo/queries", new MockRequest()
            .parameter("limit", "on").parameter("db", "oracle")).getParsedHtml().getDocument();
        assertFalse(oracle.select(".demo-query-sql").text().contains("LIMIT"), "Oracle SQL omits the unsupported LIMIT");
        assertEquals(1, oracle.select(".demo-query-note").size(), "Oracle shows the unsupported-feature note");
        assertTrue(oracle.select(".demo-query-note").text().contains("Oracle"), "the note names the database");
    }

    @Test
    void verifyWorkflowDemo() {
        var m = new MockConversation(new RifersSite());
        // the initial stage invites an order and shows an empty feed
        var doc = m.doRequest("/demo/workflow").getParsedHtml().getDocument();
        assertFalse(doc.select(".demo-wf-place").isEmpty(), "the stage offers a place-order button");
        assertFalse(doc.select(".demo-wf-empty").text().isEmpty(), "the feed starts with an empty-state hint");
        assertEquals(0, doc.select(".demo-wf-row").size(), "no orders have been placed yet");
        // placing an order runs the three tasks and returns just the new order card
        var card = m.doRequest("/demo/workflow", new MockRequest().parameter("place", "1")).getParsedHtml().getDocument();
        var rows = card.select(".demo-wf-row");
        assertEquals(5, rows.size(), "the parcel passes through warehouse, courier and notifier");
        assertTrue(rows.get(0).hasClass("demo-wf-warehouse") && rows.get(0).text().contains("picked"),
            "the warehouse picks and packs first");
        assertTrue(rows.get(2).hasClass("demo-wf-courier"), "the courier takes over once the box is packed");
        assertTrue(rows.get(4).hasClass("demo-wf-notifier") && rows.get(4).text().contains("on its way"),
            "the notifier emails the customer last");
        assertFalse(card.select(".demo-wf-order-no").text().isEmpty(), "the order card shows its order number");
    }

    @Test
    void verifyQueryManagerDemo() {
        var m = new MockConversation(new RifersSite());
        // the seeded books are restored through the manager
        var seeded = m.doRequest("/demo/gqm").getParsedHtml().getDocument();
        assertEquals(2, seeded.select(".demo-gqm-row").size(), "two seed books are restored");
        assertTrue(seeded.select(".demo-gqm-list").text().contains("Effective Java"), "a seed book is listed");
        // adding a book saves it and reads it straight back with a generated id
        var added = m.doRequest("/demo/gqm", new MockRequest()
            .parameter("title", "Refactoring").parameter("author", "Martin Fowler")).getParsedHtml().getDocument();
        assertEquals(3, added.select(".demo-gqm-row").size(), "the saved book joins the list");
        assertTrue(added.select(".demo-gqm-list").text().contains("Refactoring"), "the saved book is restored");
        assertEquals(1, added.select(".demo-gqm-saved").size(), "the generated id is reported");
    }

    @Test
    void verifyQueryManagerDemoIsStateless() {
        var m = new MockConversation(new RifersSite());
        // every request rebuilds the throwaway database, so nothing accumulates
        for (var i = 0; i < 5; i++) {
            var doc = m.doRequest("/demo/gqm", new MockRequest().parameter("title", "Book " + i))
                .getParsedHtml().getDocument();
            assertEquals(3, doc.select(".demo-gqm-row").size(), "each run starts from the two seeds");
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
    void verifyDemoSourceLinks() {
        var prefix = "https://github.com/rife2/rifers/blob/main/";
        // every inline demo on the home page links to the specific file that powers it
        var home = page("/").select(".demo-inline .demo-src");
        assertEquals(15, home.size(), "each home demo should link to its source");
        for (var a : home) {
            var href = a.attr("href");
            assertTrue(href.startsWith(prefix + "src/main/java/rifers/elements/demo/"),
                "source link points at the rifers demo package: " + href);
            var file = href.substring(prefix.length());
            assertTrue(Files.exists(Path.of(file)), "linked source file must exist: " + file);
        }
        // every demo except JSON (which is a JSON API, no template) also links to its template
        var homeTpl = page("/").select(".demo-inline .demo-tpl");
        assertEquals(14, homeTpl.size(), "each home demo with a template links to it");
        for (var a : homeTpl) {
            var href = a.attr("href");
            assertTrue(href.startsWith(prefix + "src/main/resources/templates/demo/") && href.endsWith(".html"),
                "template link points at a demo template: " + href);
            assertTrue(Files.exists(Path.of(href.substring(prefix.length()))), "linked template must exist: " + href);
        }
        // the bld configurator links to both its source and its template
        var bld = page("/bld").select(".bld-config .demo-src");
        assertEquals(1, bld.size(), "the configurator should link to its source");
        assertTrue(bld.attr("href").endsWith("BldConfigDemo.java"), "configurator links to BldConfigDemo.java");
        assertTrue(Files.exists(Path.of(bld.attr("href").substring(prefix.length()))), "configurator source file must exist");
        var bldTpl = page("/bld").select(".bld-config .demo-tpl");
        assertEquals(1, bldTpl.size(), "the configurator should link to its template");
        assertTrue(bldTpl.attr("href").endsWith("bldconfig.html"), "configurator links to bldconfig.html");
        assertTrue(Files.exists(Path.of(bldTpl.attr("href").substring(prefix.length()))), "configurator template must exist");
    }

    @Test
    void verifyBldConfigDemo() {
        var m = new MockConversation(new RifersSite());
        // the default application build file: a Build class, a derived main class, JUnit tests
        var def = m.doRequest("/demo/bld-config").getParsedHtml().getDocument();
        var defCode = def.selectFirst(".bld-config-out").text();
        assertTrue(defCode.contains("class MyAppBuild extends Project"), "project name derives the build class");
        assertTrue(defCode.contains("mainClass = \"com.example.MyApp\""), "the main class is derived");
        assertTrue(defCode.contains("junit-jupiter"), "JUnit is included by default");
        assertFalse(defCode.contains("scope(compile)"), "no compile dependency by default");
        // a custom package and name flow into the code; both compile deps share one scope; tests off drops JUnit
        var custom = m.doRequest("/demo/bld-config", new MockRequest()
            .parameter("name", "order service").parameter("pkg", "com.acme")
            .parameter("logging", "true").parameter("commons", "true").parameter("junit", "false"))
            .getParsedHtml().getDocument();
        var customCode = custom.selectFirst(".bld-config-out").text();
        assertTrue(customCode.contains("package com.acme;"), "the package name flows through");
        assertTrue(customCode.contains("class OrderServiceBuild extends Project"), "multi-word names become a valid class");
        assertTrue(customCode.contains("org.slf4j"), "the logging dependency is added");
        assertTrue(customCode.contains("commons-lang3"), "the commons dependency is added");
        assertEquals(1, StringUtils.count(customCode, "scope(compile)"), "both compile deps share a single scope(compile)");
        assertFalse(customCode.contains("junit-jupiter"), "JUnit is dropped when tests are off");
        // the RIFE2 web app type generates a full WebProject
        var webCode = m.doRequest("/demo/bld-config", new MockRequest().parameter("type", "web"))
            .getParsedHtml().getDocument().selectFirst(".bld-config-out").text();
        assertTrue(webCode.contains("extends WebProject"), "web app extends WebProject");
        assertTrue(webCode.contains("com.uwyn.rife2"), "web app depends on rife2");
        assertTrue(webCode.contains("uberJarMainClass"), "web app sets the uber jar main class");
        assertTrue(webCode.contains("scope(standalone)"), "web app has a standalone scope");
        assertTrue(webCode.contains("jetty-ee10"), "web app bundles embedded Jetty");
        assertTrue(webCode.contains("precompileOperation()"), "web app precompiles templates");
        // the library type omits the main class assignment
        var libCode = m.doRequest("/demo/bld-config", new MockRequest().parameter("type", "lib"))
            .getParsedHtml().getDocument().selectFirst(".bld-config-out").text();
        assertTrue(libCode.contains("class MyAppBuild extends Project"), "library still extends Project");
        assertFalse(libCode.contains("mainClass ="), "a library has no main class assignment");
        // hostile input still yields valid Java: digit-first and keyword package
        // segments become valid identifiers, and a quote in the name is escaped
        // inside the string literal instead of breaking it
        var hostileCode = m.doRequest("/demo/bld-config", new MockRequest()
            .parameter("pkg", "123.class").parameter("name", "bad\"name"))
            .getParsedHtml().getDocument().selectFirst(".bld-config-out").text();
        assertTrue(hostileCode.contains("package _123._class;"), "invalid package segments become valid identifiers");
        assertTrue(hostileCode.contains("name = \"bad\\\"name\""), "a quote in the name is escaped inside the Java string");
        assertFalse(hostileCode.contains("\"bad\"name\""), "the unescaped quote never reaches the output");
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
    void verifyMigrationsDemo() {
        var m = new MockConversation(new RifersSite());
        // version 1: users(id, login), no email yet
        var v1 = m.doRequest("/demo/migrations", new MockRequest().parameter("to", "1")).getParsedHtml().getDocument();
        var v1schema = v1.select(".demo-schema").text().toLowerCase();
        assertTrue(v1schema.contains("users"), "version 1 should have the users table");
        assertTrue(v1schema.contains("login"), "version 1 should have the login column");
        assertFalse(v1schema.contains("email"), "version 1 should not have email yet");
        // version 2: the email column is added
        var v2 = m.doRequest("/demo/migrations", new MockRequest().parameter("to", "2")).getParsedHtml().getDocument();
        assertTrue(v2.select(".demo-schema").text().toLowerCase().contains("email"), "version 2 should add the email column");
        // rolling back from 2 to 1 genuinely runs down(): the users table stays but email is gone
        var backResponse = m.doRequest("/demo/migrations",
            new MockRequest().parameter("to", "1").parameter("from", "2"));
        assertEquals(200, backResponse.getStatus(), "rollback should succeed, not error");
        var back = backResponse.getParsedHtml().getDocument().select(".demo-schema").text().toLowerCase();
        assertTrue(back.contains("users") && back.contains("login"), "rollback should keep the users table at version 1");
        assertFalse(back.contains("email"), "rolling back should drop the email column");
        // version 0: empty database
        var v0 = m.doRequest("/demo/migrations", new MockRequest().parameter("to", "0")).getParsedHtml().getDocument();
        assertEquals(0, v0.select(".demo-schema-table").size(), "version 0 should have no tables");
    }

    @Test
    void verifyMigrationsDemoIsStateless() {
        var m = new MockConversation(new RifersSite());
        // each request must use its own throwaway database; repeated runs all succeed
        for (var i = 0; i < 30; i++) {
            var response = m.doRequest("/demo/migrations", new MockRequest().parameter("to", "2"));
            assertEquals(200, response.getStatus(), "request " + i + " should succeed");
            assertTrue(response.getText().toLowerCase().contains("email"), "each fresh database should reach version 2");
        }
    }

    @Test
    void verifyHelloWorldDemo() {
        var doc = page("/demo/hello");
        assertTrue(doc.select(".demo-stage").text().contains("Hello World"), "hello demo should show the response");
    }

    @Test
    void verifyTypeSafeLinkDemo() {
        var doc = page("/demo/link");
        // urlFor(hello) generated the link and its shown URL, both pointing at the hello route
        var link = doc.select(".demo-stage a").first();
        assertNotNull(link, "link demo should render the generated link");
        assertTrue(link.attr("href").endsWith("/demo/hello"), "link should point at the hello route");
        assertTrue(doc.select(".demo-url").text().endsWith("/demo/hello"), "the generated URL should be shown");
    }

    @Test
    void verifyCounterDemo() {
        var m = new MockConversation(new RifersSite());
        var r1 = m.doRequest("/demo/counter");
        assertEquals(200, r1.getStatus());
        assertEquals("1", r1.getParsedHtml().getDocument().selectFirst(".demo-counter").text(),
            "counter should start at 1");
        // following the add link resumes the paused continuation with count preserved
        var r2 = r1.getParsedHtml().getLinkWithText("Add one").follow();
        assertEquals("2", r2.getParsedHtml().getDocument().selectFirst(".demo-counter").text(),
            "the continuation should resume where it paused and increment");
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

    @Test
    void verifyDemosServeHtmxFragments() {
        var m = new MockConversation(new RifersSite());
        // every live demo serves the whole page on a normal visit and just the
        // stage fragment to htmx, via RIFE2's isHxRequest() + printBlock support
        for (var path : new String[]{"/demo/hello", "/demo/link", "/demo/counter", "/demo/csrf",
            "/demo/templates", "/demo/validation", "/demo/migrations", "/demo/queries",
            "/demo/gqm", "/demo/test", "/demo/workflow", "/demo/sse"}) {
            var full = m.doRequest(path).getText();
            assertTrue(full.contains("<html"), "a plain request serves the full page: " + path);

            var fragment = m.doRequest(path, new MockRequest().htmx());
            assertFalse(fragment.getText().contains("<html"), "an htmx request omits the document: " + path);
            assertTrue(fragment.getText().contains("demo-stage"), "an htmx request serves the stage: " + path);
            assertEquals("HX-Request", fragment.getHeader("Vary"), "the response varies on HX-Request: " + path);
        }
    }

    @Test
    void verifyFormDemoSwapsOnlyItsResult() {
        var m = new MockConversation(new RifersSite());
        // the form swaps a sub-fragment, so an htmx POST returns just the result
        var result = m.doRequest("/demo/form",
            new MockRequest().method(POST).htmx().parameter("name", "Ada"));
        var text = result.getText();
        assertTrue(text.contains("demo-result") && text.contains("Hello Ada"), "the greeting fragment comes back");
        assertFalse(text.contains("demo-form"), "the form itself is not re-sent");
        assertFalse(text.contains("<html"), "no surrounding document");
        assertEquals("HX-Request", result.getHeader("Vary"), "the result fragment varies on HX-Request");
    }

    @Test
    void verifyGqmFiresHxTrigger() {
        var m = new MockConversation(new RifersSite());
        // saving a book fires an HX-Trigger client event carrying the record as JSON
        var response = m.doRequest("/demo/gqm", new MockRequest()
            .htmx().parameter("title", "Refactoring").parameter("author", "Martin Fowler"));
        var trigger = response.getHeader("HX-Trigger");
        assertNotNull(trigger, "saving a book fires an HX-Trigger event");
        assertTrue(trigger.contains("bookSaved"), "the event is named bookSaved: " + trigger);
        assertTrue(trigger.contains("\"title\":\"Refactoring\""), "the record is serialized to JSON: " + trigger);
        // a plain read that saves nothing fires no event
        assertNull(m.doRequest("/demo/gqm", new MockRequest().htmx()).getHeader("HX-Trigger"),
            "no event when nothing is saved");
    }
}
