package codeu.chat.util;
import java.io.IOexception;
import static org.junit.Assert.*;
public final class TokenizerTest {
	@Test
	public void testWithNoQuotes() throws IOexception {
		final Tokenizer tokenizer = new Tokenizer("hello world how are you");
		assertEquals(tokenizer.next(), "hello");
		assertEquals(tokenizer.next(), "world");
		assertEquals(tokenizer.next(), "how");
		assertEquals(tokenizer.next(), "are");
		assertEquals(tokenizer.next(), "you");
		assertEquals(tokenizer.next(), null);
	}
	@Test
	public void testWithQuotes() throws IOexception {
		final Tokenizer tokenizer = new Tokenizer("\"hello world\" \"how are you\"");
		assertEquals(tokenizer.next(), "hello world");
		assertEquals(tokenizer.next(), "how are you");
		assertEquals(tokenizer.next(), null);
	}

}