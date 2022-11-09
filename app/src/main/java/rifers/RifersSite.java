package rifers;

import rife.config.RifeConfig;
import rife.engine.*;
import rifers.elements.Home;

public class RifersSite extends Site {
    public final Route home = get("/", Home.class);

    public static void main(String[] args) {
        RifeConfig.engine().setProxyRootUrl("https://rife2.com");
        new Server()
            .staticResourceBase("src/main/webapp")
            .start(new RifersSite());
    }
}

