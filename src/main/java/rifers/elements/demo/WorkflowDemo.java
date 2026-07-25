package rifers.elements.demo;

import rife.engine.Context;
import rife.engine.Element;
import rife.workflow.Work;
import rife.workflow.Workflow;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A workflow-engine demo framed as order fulfillment. Three independent tasks,
 * a warehouse, a courier and a notifier, are each written as plain, sequential
 * Java. None of them manages threads, locks or queues. Each one pauses on an event
 * with pauseForEvent and resumes when the previous step triggers that event,
 * coordinated by RIFE2's continuations. Placing an order runs one workflow to
 * completion inside the request, and the trace shows the parcel handed down the
 * line as each task wakes up in turn.
 */
public class WorkflowDemo implements Element {
    private static final Object PACKED = "packed";
    private static final Object SHIPPED = "shipped";

    // a shared, ever-climbing order number, like a real shop's counter
    private static final AtomicInteger ORDER_NO = new AtomicInteger(1041);

    // one shared, daemon-threaded pool runs every order's workflow, created
    // once for the app rather than allocated per click; idle threads are reaped
    private static final ExecutorService WORKFLOW_EXECUTOR = Executors.newCachedThreadPool(runnable -> {
        var thread = new Thread(runnable, "workflow-demo");
        thread.setDaemon(true);
        return thread;
    });

    private record Product(String label, String phrase) {}

    private static final List<Product> CATALOG = List.of(
        new Product("Mechanical keyboard", "a mechanical keyboard"),
        new Product("Studio headphones", "a pair of studio headphones"),
        new Product("Cast-iron skillet", "a cast-iron skillet"),
        new Product("Watercolor set", "a watercolor set"),
        new Product("Desk lamp", "a desk lamp"),
        new Product("French press", "a French press")
    );

    public void process(Context c) {
        var t = c.template("demo/workflow");

        // a placed order runs its own fulfillment workflow and returns just the
        // new order card, which htmx prepends to the running feed
        if (c.hasParameterValue("place")) {
            var orderNo = ORDER_NO.incrementAndGet();
            var product = CATALOG.get(orderNo % CATALOG.size());
            var aisle = 1 + orderNo % 12;
            var log = runFulfillment(product, aisle);

            t.setValue("order_no", orderNo);
            t.setValueEncoded("product", product.label());
            var i = 0;
            for (var row : log) {
                t.setValue("who", row[0]);
                t.setValueEncoded("who_label", row[2]);
                t.setValueEncoded("message", row[1]);
                t.setValue("delay", (i * 120) + "ms");
                t.appendBlock("trace", "trace_row");
                i++;
            }
            c.printBlock(t, "order_card");
            return;
        }

        t.setValue("place_url", c.urlFor(c.route()).param("place", 1).toString());
        c.printHtmxFragment(t, "stage");
    }

    // each log row is {who, message, who_label}
    private static List<String[]> runFulfillment(Product product, int aisle) {
        var log = new CopyOnWriteArrayList<String[]>();

        // run the coordination off the request thread: the web engine already
        // wraps this request in its own continuation context and the workflow
        // needs its own, and a stuck run then can never tie up the request
        var run = WORKFLOW_EXECUTOR.submit(() -> {
            var workflow = new Workflow(WORKFLOW_EXECUTOR);
            // start the waiting tasks first, then the warehouse kicks the chain
            // off; trigger retains its event, so a step that has not paused yet
            // still receives it
            workflow.start(new Notifier(log));
            workflow.waitForPausedWork();
            workflow.start(new Courier(log));
            workflow.waitForPausedWork();
            workflow.start(new Warehouse(log, product, aisle));
            workflow.waitForNoWork();
            return null;
        });
        try {
            run.get(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException | TimeoutException e) {
            run.cancel(true);
        }
        return log;
    }

    // each task is ordinary sequential code, it just pauses on an event

    static class Warehouse implements Work {
        private final List<String[]> log;
        private final Product product;
        private final int aisle;

        Warehouse(List<String[]> log, Product product, int aisle) {
            this.log = log;
            this.product = product;
            this.aisle = aisle;
        }

        public void execute(Workflow w) {
            log.add(new String[]{"warehouse", "picked " + product.phrase() + " from aisle " + aisle, "Warehouse"});
            log.add(new String[]{"warehouse", "packed and sealed the box", "Warehouse"});
            w.trigger(PACKED);          // hand the box to the courier
        }
    }

    static class Courier implements Work {
        private final List<String[]> log;

        Courier(List<String[]> log) {
            this.log = log;
        }

        public void execute(Workflow w) {
            pauseForEvent(PACKED);      // suspend until the box is packed
            log.add(new String[]{"courier", "printed the shipping label", "Courier"});
            log.add(new String[]{"courier", "handed the parcel to the driver", "Courier"});
            w.trigger(SHIPPED);         // let the notifier know it is on its way
        }
    }

    static class Notifier implements Work {
        private final List<String[]> log;

        Notifier(List<String[]> log) {
            this.log = log;
        }

        public void execute(Workflow w) {
            pauseForEvent(SHIPPED);     // suspend until the parcel ships
            log.add(new String[]{"notifier", "emailed you: your order is on its way", "Notifier"});
        }
    }
}
