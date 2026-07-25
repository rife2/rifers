package rifers.elements.demo;

import rife.config.RifeConfig;
import rife.engine.Context;
import rife.engine.Element;
import rife.engine.RequestMethod;
import rife.engine.Site;
import rife.engine.annotations.Parameter;
import rife.engine.elements.CsrfProtected;
import rife.test.MockConversation;
import rife.test.MockRequest;

/**
 * A CSRF-protection demo that runs the real verification out of container.
 * A tiny site guards a state-changing POST with {@link CsrfProtected}; the
 * demo submits that POST both with the legitimate token the server handed
 * out and with a forged one, so the visitor can watch the genuine request
 * succeed and the cross-site forgery get refused with a 403.
 */
public class CsrfDemo implements Element {
    // the protected application: a state-changing transfer behind CsrfProtected
    static class Bank extends Site {
        public void setup() {
            before(new CsrfProtected());
            get("/form", c -> c.print("ready"));
            post("/transfer", c -> c.print("transferred $" + c.parameter("amount")));
        }
    }

    @Parameter String forge = "false";

    public void process(Context c) {
        var t = c.template("demo/csrf");
        t.setValue("run_url", c.urlFor(c.route()).toString());

        // nothing has been submitted yet: show the scenario and the two buttons,
        // with no outcome until the visitor actually presses one of them
        if (!c.hasParameterValue("forge")) {
            c.printHtmxFragment(t, "stage");
            return;
        }

        var forged = "true".equals(forge);

        var m = new MockConversation(new Bank());
        // a safe GET establishes the token: it sets the HttpOnly cookie, and is
        // where a real page would also receive it through the route:inputs: tag
        m.doRequest("/form");
        var realToken = m.getCookieValue(RifeConfig.engine().getCsrfCookieName());
        // the victim's browser still sends the cookie, but a cross-site attacker
        // can't read the token to put in the form, so it has to guess
        var submitted = forged ? "forged-token" : realToken;

        var response = m.doRequest("/transfer", new MockRequest()
            .method(RequestMethod.POST)
            .parameter("amount", "100")
            .parameter(RifeConfig.engine().getCsrfParameterName(), submitted));

        if (response.getStatus() == Context.SC_FORBIDDEN) {
            t.setBlock("result", "result_refused");
        } else {
            t.setValueEncoded("outcome", response.getText());
            t.setBlock("result", "result_accepted");
        }
        c.printHtmxFragment(t, "stage");
    }
}
