import rife.engine.*;

public class HelloWorld extends Site {
    public void setup() {
        get("/hello", c -> c.print("Hello World"));
    }

    public static void main(String[] args) {
        new Server().start(new HelloWorld());
    }
}