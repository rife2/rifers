import rife.test.MockConversation;

class HelloTest {
    @Test void verifyHelloWorld() {
        var m = new MockConversation(new HelloWorld());
        assertEquals("Hello World",
                     m.doRequest("/hello").getText());
    }
}