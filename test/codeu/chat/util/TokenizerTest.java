package codeu.chat.util;
import codeu.chat.util.Tokenizer;
import org.junit.Test;
import java.io.IOException;
import static org.junit.Assert.*;
public final class TokenizerTest {
	@Test
	Scanner input = new Scanner(System.in);
	public void testWithNoQuotes() throws IOException {
		final Tokenizer tokenizer = new Tokenizer();
		while (tokenizer.hasNext()){
			assertEquals(tokenizer.next(), input.next());
		}
	}
	@Test
	public void testWithQuotes() throws IOException {
		final Tokenizer tokenizer = new Tokenizer("\"hello world\" \"how are you\"");
		while (tokenizer.hasNext){
			int first_quote = 0;
			int first_quote_location = 0;
			for (int i= 0; i < tokenizer.length(); i++){
				if (tokenizer.get(i).equals("\"")){
					first_quote++;
					first_quote_location = i;
				}
				if (tokenizer.get(i).equals(" ") && ((first_quote % 2) != 0){
					assertEquals(tokenizer.next(), tokenizer.substring(first_quote_location, i));
				}
				else if (i = tokenizer.length-1){
					assertEquals(tokenizer.next(), tokenizer.substring(first_quote_location, i));
				}	
			}
		}
	}

}