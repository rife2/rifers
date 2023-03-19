package rifers;

import rife.engine.*;

public class RifersSiteUber extends RifersSite {
    public static void main(String[] args) {
        new Server()
            .staticUberJarResourceBase("webapp")
            .start(new RifersSiteUber());
    }
}