package rifers.elements.demo;

import rife.engine.*;
import rife.template.TemplateFactory;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The live, runnable versions of the quickstart examples, mounted under /demo.
 * Each demo is stateless and safe for any number of simultaneous visitors.
 */
public class DemoRouter extends Router {
    // public so templates can reference them by name through route: tags,
    // e.g. route:.demos.sseStream from the home page
    public Route hello;
    public Route link;
    public Route json;
    public Route form;
    public Route counter;
    public Route templates;
    public Route validation;
    public Route migrations;
    public Route queries;
    public Route gqm;
    public Route csrf;
    public Route test;
    public Route bldConfig;
    public Route sse;
    public Route sseStream;
    public Route sseTap;
    public Route workflow;
    public Route htmx;

    static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    final SseBroadcaster broadcaster = new SseBroadcaster();
    // a single server-wide tally shared by everyone watching the SSE demo
    final AtomicInteger taps = new AtomicInteger();
    ScheduledExecutorService ticker;

    public void setup() {
        hello = get("/hello", c -> c.printHtmxFragment(c.template("demo/hello"), "stage"));
        link = get("/link", c -> {
            var t = c.template("demo/link");
            // the URL is generated type-safe from the route, exactly like the example
            t.setValue("url", c.urlFor(hello).toString());
            c.printHtmxFragment(t, "stage");
        });
        json = get("/json", JsonDemo.class);
        form = getPost("/form", FormDemo.class);
        counter = getPost("/counter", CounterDemo.class);
        templates = get("/templates", TemplatesDemo.class);
        validation = getPost("/validation", ValidationDemo.class);
        migrations = get("/migrations", MigrationsDemo.class);
        queries = get("/queries", QueriesDemo.class);
        gqm = get("/gqm", GqmDemo.class);
        csrf = get("/csrf", CsrfDemo.class);
        test = get("/test", TestDemo.class);
        workflow = get("/workflow", WorkflowDemo.class);
        htmx = get("/htmx", HtmxDemo.class);
        bldConfig = get("/bld-config", BldConfigDemo.class);
        sseStream = get("/sse/stream", c -> c.sse(broadcaster));
        sse = get("/sse", c -> {
            var t = c.template("demo/sse");
            t.setValueEncoded("time", TIME_FORMAT.format(LocalTime.now()));
            t.setBlock("clock_row", "clock_row");
            t.setValue("taps", taps.get());
            t.setBlock("taps_row", "taps_row");
            c.printHtmxFragment(t, "stage");
        });
        // a tap from any visitor bumps the shared tally and broadcasts the new
        // total to every connected client, so all open windows update at once
        sseTap = post("/sse/tap", c -> {
            var t = TemplateFactory.HTML.get("demo/sse");
            t.setValue("taps", taps.incrementAndGet());
            broadcaster.send(new ServerSentEvent().name("taps").templateBlock(t, "taps_row"));
        });

        // a single server-wide ticker pushes the current time to every
        // connected client as an htmx-swappable template block; it doesn't
        // matter how many people watch at once, they share this one broadcast
        ticker = Executors.newSingleThreadScheduledExecutor(runnable -> {
            var thread = new Thread(runnable, "sse-ticker");
            thread.setDaemon(true);
            return thread;
        });
        ticker.scheduleAtFixedRate(() -> {
            var t = TemplateFactory.HTML.get("demo/sse");
            t.setValueEncoded("time", TIME_FORMAT.format(LocalTime.now()));
            broadcaster.send(new ServerSentEvent().name("clock").templateBlock(t, "clock_row"));
        }, 1, 1, TimeUnit.SECONDS);
    }

    public void destroy() {
        if (ticker != null) {
            ticker.shutdownNow();
        }
        broadcaster.close();
    }
}
