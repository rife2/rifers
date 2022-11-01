package rifers;

import rife.engine.Route;
import rife.engine.Site;
import rife.server.Server;
import rifers.elements.Home;

public class RifersSite extends Site {
    public final Route home = get("/", Home.class);

    public static void main(String[] args) {
        new Server()
            .port(4242)
            .staticResourceBase("src/main/webapp")
            .start(new RifersSite());
    }
}

