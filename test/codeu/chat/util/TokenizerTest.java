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

	private String emptyString() throws IOException {
		if (token.equals("")){
			return "Empty String. Please try again.";
		}
		else{
			return token.toString();
		}
	}

	private String oneQuotation() throws IOException {
		if (token.substring(0, 1) == "\"" && token.substring(token.length()-1, token.length()) != "\""){
			return "Missing closing quotation. Please try again.";
		}
		else return token.toString();
	}

	private String hasWhiteSpaces() throws IOException {
		for(int i=0; i < token.length(); i++){
			if (token.substring(i, i+1).equals(" ")){
				return "Has a space. Please try again with no spaces.";
			}
		}
		return token.toString();
	}

	private String noText() throws IOException {
		if (token.length() == 0){
			return "No Text. Please try again.";
		}
		else{
			return token.toString();
		}
	}

}