package rifers.elements.demo;

import rife.engine.Context;
import rife.engine.Element;

/**
 * A web-continuations demo: the loop pauses on every click and resumes exactly
 * where it left off, with its local {@code count} preserved by RIFE2. Each
 * visitor's paused state lives in their own continuation (keyed by the contId
 * that {@code urlFor} stamps into the link), so any number of people can count
 * at the same time without touching each other's state.
 * <p>
 * The template is rendered from a helper method that returns before {@code
 * pause()}, so the continuation only ever captures the {@code count} int.
 */
public class CounterDemo implements Element {
    public void process(Context c) {
        var count = 0;
        while (count < 10) {
            count++;
            render(c, count);
            // suspend the running program here until the next request, the
            // local count survives and the loop resumes on this exact line
            c.pause();
        }
        renderDone(c);
    }

    private void render(Context c, int count) {
        var t = c.template("demo/counter");
        t.setValue("count", count);
        t.setValue("action", c.urlFor(c.route()).toString());
        t.setBlock("body", "counting");
        c.printHtmxFragment(t, "stage");
    }

    private void renderDone(Context c) {
        var t = c.template("demo/counter");
        t.setBlock("body", "done");
        c.printHtmxFragment(t, "stage");
    }
}
