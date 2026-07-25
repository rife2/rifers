import rife.test.MockConversation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GreeterTest {
    @Test void submitsTheForm() {
        var m = new MockConversation(new Greeter());

        // find the form in the HTML, fill it, and submit it, all in-process
        var form = m.doRequest("/greet")
                    .getParsedHtml()
                    .getFormWithName("greet");
        form.setParameter("who", "Ada");

        var response = form.submit();
        assertEquals("Hello Ada!", response.getText());
    }
}
