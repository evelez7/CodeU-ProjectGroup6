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
	}
	@Test
	public String emptyString() throws IOException {
		final Tokenizer tokenizer = new Tokenizer();
		if (tokenizer.equals("")){
			return "Empty String. Please try again.";
		}
		else{
			return token.toString();
		}
	}
	@Test
	public String oneQuotation() throws IOException {
		final Tokenizer tokenizer = new Tokenizer();
		if (tokenizer.substring(0, 1) == "\"" && tokenizer.substring(tokenizer.length()-1, tokenizer.length()) != "\""){
			return "Missing closing quotation. Please try again.";
		}
		else return token.toString();
	}
	@Test
	public String hasWhiteSpaces() throws IOException {
		final Tokenizer tokenizer = new Tokenizer();
		for(int i=0; i < token.length(); i++){
			if (tokenizer.substring(i, i+1).equals(" ")){
				return "Has a space. Please try again with no spaces.";
			}
		}
		return token.toString();
	}
	@Test
	public String noText() throws IOException {
		final Tokenizer tokenizer = new Tokenizer();
		if (tokenizer.length() == 0){
			return "No Text. Please try again.";
		}
		else{
			return tokenizer.toString();
		}
	}

}