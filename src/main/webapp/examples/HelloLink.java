import rife.engine.*;

public class HelloLink extends Site {
    Route hello = get("/hello", c -> c.print("Hello World"));
    Route link  = get("/link",  c -> c.print(
        "<a href='" + c.urlFor(hello) + "'>Hello</a>"));

    public static void main(String[] args) {
        new Server().start(new HelloLink());
    }
}
