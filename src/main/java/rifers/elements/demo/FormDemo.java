package rifers.elements.demo;

import rife.engine.Context;
import rife.engine.Element;
import rife.engine.annotations.Parameter;

import static rife.engine.RequestMethod.GET;

public class FormDemo implements Element {
    @Parameter String name;

    public void process(Context c) {
        var t = c.template("demo/form");
        t.setValue("run_url", c.urlFor(c.route()).toString());
        // the widget's form has no action, so a POST lands right back here
        if (c.method() != GET) {
            t.setValueEncoded("name", name);
            t.setBlock("greeting", "greeting_filled");
        }
        t.setBlock("result", "result_content");
        // the form swaps only its result on an htmx POST, the whole stage
        // otherwise. the response differs on HX-Request, so declare that for
        // caches; printHtmxFragment does the same on its own path
        c.varyOn("HX-Request");
        if (c.isHxRequest() && c.method() != GET) {
            c.printBlock(t, "result_content");
        } else {
            c.printHtmxFragment(t, "stage");
        }
    }
}
