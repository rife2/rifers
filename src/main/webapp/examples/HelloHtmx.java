import rife.engine.*;

import java.util.List;

public class HelloHtmx extends Site {
    static final List<String> LANGUAGES = List.of("Java", "Kotlin", "Scala", "Groovy", "Clojure");

    public void setup() {
        get("/languages", c -> {
            var t = c.template("languages");
            var q = c.parameter("q", "").toLowerCase();
            for (var name : LANGUAGES) {
                if (name.toLowerCase().contains(q)) {
                    t.setValueEncoded("name", name);
                    t.appendBlock("rows", "row");
                }
            }
            // a browser gets the whole page, htmx gets just the "list" block
            c.printHtmxFragment(t, "list");
        });
    }

    public static void main(String[] args) {
        new Server().start(new HelloHtmx());
    }
}
