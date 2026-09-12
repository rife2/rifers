package rifers;

import org.jsoup.nodes.Document;
import rife.test.MockConversation;

import java.net.URL;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

// what the site's test classes share: where the assets live, which demo pages
// exist, and rendering a page through an in-memory conversation
public class SiteTestSupport {
    static final String WEBAPP = "src/main/webapp";
    static final String[] DEMO_PATHS = {"/demo/hello", "/demo/link", "/demo/form", "/demo/counter",
        "/demo/validation", "/demo/csrf", "/demo/templates", "/demo/htmx", "/demo/sse",
        "/demo/workflow", "/demo/migrations", "/demo/queries", "/demo/gqm", "/demo/test",
        "/demo/bld-config"};

    Document page(String path) {
        var m = new MockConversation(new RifersSite());
        var response = m.doRequest(path);
        assertEquals(200, response.getStatus(), "expected " + path + " to render");
        return response.getParsedHtml().getDocument();
    }

    static URL toUrl(String file) {
        try {
            return Path.of(file).toUri().toURL();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
