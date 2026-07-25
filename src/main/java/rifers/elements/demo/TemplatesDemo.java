package rifers.elements.demo;

import rife.engine.Context;
import rife.engine.Element;

/**
 * A bidirectional-templates demo. The template is pure HTML with named blocks
 * and values, and no logic at all. Java assembles the page: it picks a block
 * with setBlock, and builds the tag list by appending a block in a loop. The
 * demo shows the unchanging template next to the result, so the same template
 * visibly renders two different pages purely because the Java picks a different
 * block; the example listing above highlights the branch that just ran.
 */
public class TemplatesDemo implements Element {
    public void process(Context c) {
        var state = c.parameter("state", "welcome");
        var tags = c.parameter("tags", "").split(",");

        var t = c.template("demo/templates");
        t.setValue("run_url", c.urlFor(c.route()).toString());
        // the form starts with sample tags on first load, then echoes back
        // exactly what was submitted, the query itself takes whatever comes in
        t.setValueEncoded("tags_input", c.hasParameterValue("tags") ? c.parameter("tags") : "java, rife2, bld");

        if ("dashboard".equals(state)) {
            t.setValue("state_dashboard", " selected");
            t.setValueEncoded("name", "Ada");
            // build the tag list by appending the "tag" block once per tag, the
            // loop is Java, the template just provides the reusable piece
            for (var tag : tags) {
                t.setValueEncoded("label", tag.trim());
                t.appendBlock("tags", "tag");
            }
            t.setBlock("body", "dashboard");
        } else {
            t.setValue("state_welcome", " selected");
            t.setBlock("body", "welcome");
        }

        c.printHtmxFragment(t, "stage");
    }
}
