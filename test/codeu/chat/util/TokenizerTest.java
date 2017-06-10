package codeu.chat.util;
import java.util.*;
import codeu.chat.util.Tokenizer;
import org.junit.Test;
import java.io.IOException;
import static org.junit.Assert.*;
public final class TokenizerTest {
	@Test
	public void testWithNoQuotes() throws IOException {
		final Tokenizer tokenizer = new Tokenizer("hello world how are you");
	 	assertEquals(tokenizer.next(), "hello");
		assertEquals(tokenizer.next(), "world");
		assertEquals(tokenizer.next(), "how");
		assertEquals(tokenizer.next(), "are");
		assertEquals(tokenizer.next(), "you");
		assertEquals(tokenizer.next(), null);
	}
	@Test
	public void testWithQuotes() throws IOException {
		final Tokenizer tokenizer = new Tokenizer("\"hello world\" \"how are you\"");
		assertEquals(tokenizer.next(), "hello world");
		assertEquals(tokenizer.next(), "how are you");
		assertEquals(tokenizer.next(), null);
	}
	@Test
	public void testWithEmptyString() throws IOException {
		final Tokenizer tokenizer = new Tokenizer("");
		assertEquals(tokenizer.next(), "");
		assertEquals(tokenizer.next(), null);
	}
	@Test
	public void testWithOneQuotation() throws IOException {
		final Tokenizer tokenizer = new Tokenizer("\"hello world how are you");
		assertEquals(tokenizer.next(), "\"hello");
		assertEquals(tokenizer.next(), "world");
		assertEquals(tokenizer.next(), "how");
		assertEquals(tokenizer.next(), "are");
		assertEquals(tokenizer.next(), "you");
		assertEquals(tokenizer.next(), null);
	}
	@Test
	public void testWithWhiteSpaces() throws IOException {
		final Tokenizer tokenizer = new Tokenizer("  ");
		assertEquals(tokenizer.next(), "  ");
	}

}