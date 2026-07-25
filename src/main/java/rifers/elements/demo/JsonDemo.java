package rifers.elements.demo;

import rife.engine.Context;
import rife.engine.Element;
import rife.json.Json;

/**
 * A JSON API demo. A plain record is serialized to JSON with no annotations
 * and no configuration: its components become the JSON keys automatically.
 * The endpoint answers with a real {@code application/json} response, exactly
 * what any client would receive, and the live demo on the home page calls it
 * and shows the request and the JSON it returns.
 */
public class JsonDemo implements Element {
    record Greeting(String name, String message) {}

    public void process(Context c) {
        var raw = c.parameter("name", "world");
        var who = raw.isBlank() ? "world" : raw.trim();

        c.setContentType("application/json");
        c.print(Json.from(new Greeting(who, "Hello " + who + "!")));
    }
}
