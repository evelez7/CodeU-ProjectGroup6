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

	public int length() throws IOException{
		return source.length();
	}

	public String getInput() throws IOException{
		return this.source;
	}

	public String substring(int beginning, int end) throws IOException{
		String output = "";
		for (int i = beginning; i < end; i++){
			output += source.charAt(i);
		}
		return output;
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