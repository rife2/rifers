import rife.workflow.*;

public class HelloWorkflow {
    private static final Object PACKED = "packed", SHIPPED = "shipped";

    // each task is plain sequential code, it just pauses on an event
    static class Warehouse implements Work {
        public void execute(Workflow w) {
            System.out.println("picked and packed the box");
            w.trigger(PACKED);             // hand it to the courier
        }
    }
    static class Courier implements Work {
        public void execute(Workflow w) {
            pauseForEvent(PACKED);         // wait until the box is packed
            System.out.println("shipped the parcel");
            w.trigger(SHIPPED);            // let the notifier know
        }
    }
    static class Notifier implements Work {
        public void execute(Workflow w) {
            pauseForEvent(SHIPPED);        // wait until the parcel ships
            System.out.println("emailed: your order is on its way");
        }
    }

    public static void main(String[] args) throws InterruptedException {
        var wf = new Workflow();
        wf.start(new Notifier());
        wf.waitForPausedWork();    // start with the waiters ready for clarity
        wf.start(new Courier());
        wf.waitForPausedWork();
        wf.start(new Warehouse()); // the warehouse kicks the chain off
        wf.waitForNoWork();        // run until every task is finished
    }
}
