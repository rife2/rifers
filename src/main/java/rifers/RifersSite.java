package rifers;

import rife.config.RifeConfig;
import rife.engine.*;
import rife.template.TemplateFactory;
import rifers.elements.BldPage;
import rifers.elements.Home;
import rifers.elements.demo.DemoRouter;

import java.util.HashSet;

public class RifersSite extends Site {
    public final Route home = get("/", Home.class);
    public final Route bld = get("/bld", BldPage.class);
    // a curated map of the framework for LLMs and AI coding agents (llmstxt.org)
    public final Route llms = get("/llms.txt", c -> {
        c.setContentType("text/plain; charset=UTF-8");
        c.print(TemplateFactory.TXT.get("llms"));
    });
    public final DemoRouter demos = group("/demo", new DemoRouter());

    public void setup() {
        // .txt is passed through to static serving by default; drop it so the
        // route above owns /llms.txt instead of falling back to a static file
        var passThrough = new HashSet<>(RifeConfig.engine().getPassThroughSuffixes());
        passThrough.remove("txt");
        RifeConfig.engine().setPassThroughSuffixes(passThrough);

        if (properties().contains("rifers.production.deployment")) {
            RifeConfig.engine().setProxyRootUrl("https://rife2.com");
        }
    }

    public static void main(String[] args) {
        new Server()
            .staticResourceBase("src/main/webapp")
            .start(new RifersSite());
    }
}