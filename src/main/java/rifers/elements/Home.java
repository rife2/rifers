package rifers.elements;

import rife.engine.Context;
import rife.engine.Element;

public class Home implements Element {
    public void process(Context c) {
        var t = c.template();
        t.setValue("page_title", "RIFE2 - Full-stack, no-declaration web framework for modern Java");
        t.setValue("page_description", "RIFE2 is a full-stack, no-declaration framework to quickly and effortlessly create web applications with modern Java. No dependencies, a single 2.5MB jar, Java 17+.");
        t.setValue("page_url", "https://rife2.com/");
        t.setValue("og_image", "og-rife2.png");
        t.setValue("nav_docs", "https://github.com/rife2/rife2/wiki");
        t.setValue("nav_github", "https://github.com/rife2/rife2");
        t.setBlock("nav_active_home", "nav_active");
        c.print(t);
    }
}
