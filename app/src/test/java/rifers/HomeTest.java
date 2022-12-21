package rifers;

import org.junit.jupiter.api.Test;
import rife.test.MockConversation;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HomeTest {
    @Test
    void verifyHomePage() {
        var m = new MockConversation(new RifersSite());
        assertEquals("RIFE2 Framework : Full-stack, no-declaration, framework to quickly and effortlessly create web applications with modern Java.",
            m.doRequest("/").getParsedHtml().getTitle());
    }
}
