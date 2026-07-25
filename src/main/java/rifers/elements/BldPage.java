package rifers.elements;

import rife.engine.Context;
import rife.engine.Element;

public class BldPage implements Element {
    public void process(Context c) {
        var t = c.template("bld");
        t.setValue("page_title", "bld - pure Java build tool");
        t.setValue("page_description", "bld is a build tool that lets you write your build logic in pure Java. No DSLs, no XML, no auto-magical behavior, just plain Java code that your IDE fully understands.");
        t.setValue("page_url", "https://rife2.com/bld");
        t.setValue("og_image", "og-bld4.png");
        t.setValue("nav_docs", "https://github.com/rife2/bld/wiki");
        t.setValue("nav_github", "https://github.com/rife2/bld");
        t.setBlock("nav_active_bld", "nav_active");
        c.print(t);
    }
}
