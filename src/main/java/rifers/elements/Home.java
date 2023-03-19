package rifers.elements;

import rife.engine.Context;
import rife.engine.Element;

public class Home implements Element {
    public void process(Context c) {
        c.print(c.template());
    }
}
