package rifers;

import rife.engine.*;
import rifers.elements.Home;

public class RifersSite extends Site {
    public final Route home = get("/", Home.class);

    public static void main(String[] args) {
        new Server()
            .staticResourceBase("src/main/webapp")
            .start(new RifersSite());
    }
}

