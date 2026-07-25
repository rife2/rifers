package com.example;

import rife.engine.*;
import rife.engine.elements.CsrfProtected;

public class HelloCsrf extends Site {
    public void setup() {
        // guard every state-changing request with a CSRF token
        before(new CsrfProtected());

        // GET establishes the token, the route:inputs: tag puts it in the form
        get("/transfer", c -> c.print(c.template("transfer")));

        // POST is verified before it runs, a forged request gets 403
        post("/transfer", c -> c.print("Transferred " + c.parameter("amount")));
    }

    public static void main(String[] args) {
        new Server().start(new HelloCsrf());
    }
}
