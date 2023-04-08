package rifers;

import rife.config.RifeConfig;
import rife.engine.*;
import rife.engine.elements.PrintTemplate;
import rifers.elements.Home;

public class RifersSite extends Site {
    public final Route home = get("/", Home.class);
    public final Route bld = get("/bld", new PrintTemplate("bld"));

    public void setup() {
        if (properties().contains("rifers.production.deployment")) {
            RifeConfig.engine().setProxyRootUrl("https://rife2.com");
        }
    }

    public static void main(String[] args) {
        new Server()
            .staticResourceBase("src/main/webapp")
            .start(new RifersSite());
    }
}