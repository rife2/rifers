package rifers.elements.demo;

import rife.engine.Context;
import rife.engine.Element;
import rife.engine.Site;
import rife.engine.annotations.Parameter;
import rife.test.MockConversation;

/**
 * An out-of-container testing demo. It drives a small application the way a
 * browser would, but entirely in-process: it finds the form in the response
 * HTML, fills it in, submits it, and asserts the reply, with no servlet
 * container and no browser. You pick the value to assert against, so you can
 * watch the same test pass and fail, and see how fast the whole flow runs.
 */
public class TestDemo implements Element {
    // the tiny application under test: a form and the route that handles it
    static class Greeter extends Site {
        public void setup() {
            get("/greet", c -> c.print("""
                <html><body>
                <form name="greet" action="/greet" method="post">
                <input name="who"><input type="submit">
                </form>
                </body></html>"""));
            post("/greet", c -> c.print("Hello " + c.parameter("who") + "!"));
        }
    }

    @Parameter String expected = "Hello Ada!";

    public void process(Context c) {
        var t = c.template("demo/test");
        t.setValue("run_url", c.urlFor(c.route()).toString());
        t.setValueEncoded("expected", expected);

        // show the form with its expected value, but don't run the test until
        // the visitor actually presses "Run test"
        if (!c.hasParameterValue("expected")) {
            c.printHtmxFragment(t, "stage");
            return;
        }

        var start = System.nanoTime();
        // no server, no browser: find the form in the HTML, fill it, submit it
        var m = new MockConversation(new Greeter());
        var form = m.doRequest("/greet").getParsedHtml().getFormWithName("greet");
        form.setParameter("who", "Ada");
        var actual = form.submit().getText();
        var micros = (System.nanoTime() - start) / 1000;

        t.setValueEncoded("actual", actual);
        t.setValue("micros", micros);
        t.setBlock("result", expected.equals(actual) ? "result_pass" : "result_fail");
        c.printHtmxFragment(t, "stage");
    }
}
