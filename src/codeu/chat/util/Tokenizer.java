package codeu.chat.util;
import java.io.*;
public final class Tokenizer {
	private StringBuilder token = new StringBuilder();
	private String source;
	private int at;

	public Tokenizer(String source) {
		this.source = source;
	}

	public String next() throws IOException {
		while (remaining() > 0 && Character.isWhitespace(peek())) {
			read();
		}
		if (remaining() <= 0) {
			return null;
		} else if (peek() == '"') {
			//read a token that is surrounded by quotes
			return readWithQuotes();
		} else {
			//read a token that is not surrounded by quotes
			return readWithNoQuotes();
		}
	}

	private int remaining() {
		return source.length() - at;
	}

	private char peek() throws IOException {
		if (at < source.length()) {
			return source.charAt(at);
		} else {
			throw new IOException("too short");
		}
	}

	private char read() throws IOException {
		final char c = peek();
		at += 1;
		return c;
	}

	private String readWithNoQuotes() throws IOException {
		token.setLength(0); //clear the token
		while (remaining() > 0 && !Character.isWhitespace(peek())) {
			token.append(read());
		}
		return token.toString();
	}

	private String emptyString() throws IOException {
		if (token.equals("")){
			return "Empty String. Please try again.";
		}
	}

	private String oneQuotation() throws IOException {
		if (token.get(0) == "\"" && token.get(token.length()) != "\""){
			return "Missing closing quotation. Please try again.";
		}
	}

	private String hasWhiteSpaces() throws IOException {
		if (token.contains(" ")){
			return "Has a space. Please try again with no spaces.";
		}
	}

	private String noText() throws IOException {
		if (token.length() == 0){
			return "No Text. Please try again.";
		}
	}
	private String readWithQuotes() throws IOException {
		token.setLength(0);
		if (read() != '"') {
			throw new IOException("Strings must start with opening quote");
		}
		while(peek() != '"') {
			token.append(read());
		}
		read(); // read the closing the quote that allowed us to exit the loop
		return token.toString();
	}

}