import rife.engine.*;

public class Counter implements Element {
    public void process(Context c) {
        var count = 0;
        while (count++ < 10) {
            c.print(count);
            c.print(" <a href='" + c.urlFor(c.route()) + "'>add</a>");

            // suspend the running program here until the next request, the
            // local count survives and the loop resumes on this exact line
            c.pause();
        }
        c.print("done");
    }
}
