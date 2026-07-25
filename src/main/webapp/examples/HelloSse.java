import rife.engine.*;
import rife.template.TemplateFactory;

import java.time.LocalTime;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class HelloSse extends Site {
    final SseBroadcaster broadcaster = new SseBroadcaster();
    final AtomicInteger taps = new AtomicInteger();

    Route stream = get("/stream", c -> c.sse(broadcaster));

    // a tap from any visitor is broadcast to everyone connected
    Route tap = post("/tap", c -> {
        var t = c.template("HelloSse");
        t.setValue("taps", taps.incrementAndGet());
        broadcaster.send(new ServerSentEvent()
            .name("taps").templateBlock(t, "taps_row"));
    });

    public void setup() {
        // the clock pushes the time to everyone once per second
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            var t = TemplateFactory.HTML.get("HelloSse");
            t.setValue("time", LocalTime.now());
            broadcaster.send(new ServerSentEvent()
                .name("clock").templateBlock(t, "clock_row"));
        }, 1, 1, TimeUnit.SECONDS);
    }

    public static void main(String[] args) {
        new Server().start(new HelloSse());
    }
}
