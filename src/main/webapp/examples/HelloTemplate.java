import rife.engine.*;

public class HelloTemplate extends Site {
    public void setup() {
        get("/page", c -> {
            var t = c.template("page");
            // the template has no logic: Java picks which block to render
            if (c.hasParameterValue("name")) {
                var tags = c.parameter("tags", "").split(",");
                t.setValueEncoded("name", c.parameter("name"));
                // one appendBlock per tag builds the list, the loop is Java
                for (var tag : tags) {
                    t.setValueEncoded("label", tag.trim());
                    t.appendBlock("tags", "tag");
                }
                t.setBlock("body", "dashboard");
            } else {
                t.setBlock("body", "welcome");
            }
            c.print(t);
        });
    }

    public static void main(String[] args) {
        new Server().start(new HelloTemplate());
    }
}
