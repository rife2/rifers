import rife.engine.Site;
import rife.test.MockConversation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GreeterTest {
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

    @Test void submitsTheForm() {
        var m = new MockConversation(new Greeter());

        // find the form in the HTML, fill it, and submit it, all in-process
        var form = m.doRequest("/greet")
                    .getParsedHtml()
                    .getFormWithName("greet");
        form.setParameter("who", "Ada");

        var response = form.submit();
        assertEquals("Hello Ada!", response.getText());
    }
}
