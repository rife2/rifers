import rife.engine.*;
import rife.json.Json;

public class HelloJson extends Site {
    record Greeting(String name, String message) {}

    public void setup() {
        get("/greeting", c -> {
            var name = c.parameter("name", "world");
            c.setContentType("application/json");
            c.print(Json.from(new Greeting(name, "Hello " + name + "!")));
        });
    }

    public static void main(String[] args) {
        new Server().start(new HelloJson());
    }
}
