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
		while (tokenizer.hasNext()){
			assertEquals(tokenizer.next(), tokenizer.next());
		}
	}
	@Test
	public void testWithQuotes() throws IOException {
		final Tokenizer tokenizer = new Tokenizer("\"hello world\" \"how are you\"");
		assertEquals(tokenizer.next(), "hello world");
		assertEquals(tokenizer.next(), "how are you");
		assertEquals(tokenizer.next(), null);
		/*
		while (tokenizer.hasNext()){
			int first_quote = 0;
			int first_quote_location = 0;
			for (int i= 0; i < tokenizer.length(); i++){
				if (tokenizer.substring(i, i+1).equals("\"")){
					first_quote++;
					first_quote_location = i;
				}
				if (i == tokenizer.length-1){
					assertEquals(tokenizer.next(), tokenizer.substring(first_quote_location, i));
				}	
				else if (tokenizer.substring(i, i+1).equals(" ") && ((first_quote % 2) != 0)){
					assertEquals(tokenizer.next(), tokenizer.substring(first_quote_location, i));
				} 
			}
		}
		*/
	}

}