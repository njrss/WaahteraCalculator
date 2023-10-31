package fi.waahtera.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import lombok.Getter;
import lombok.Setter;

public class Lexer {
	
	private static final HashMap<Character, Token> VALUES = new HashMap<>();
	
	@Getter
	@Setter
	private String line;

	@Setter
	@Getter
	private boolean exitFlag;
	private boolean isNegative;

	@Setter
	private int failedCount;
	private int numberCount;
	private int index;
	
	private Scanner scanner;

	public Lexer(Scanner scanner) {
		this.scanner = scanner;
		this.line = "";
		
		VALUES.put('(', new Token(TokenType.LEFT_PAREN));
		VALUES.put(')', new Token(TokenType.RIGHT_PAREN));
		VALUES.put('+', new Token(TokenType.PLUS));
		VALUES.put('-', new Token(TokenType.MINUS));
		VALUES.put('/', new Token(TokenType.DIV));
		VALUES.put('*', new Token(TokenType.MUL));
		VALUES.put('%', new Token(TokenType.MOD));
		VALUES.put('^', new Token(TokenType.POW));
	}
	
	public boolean scanLine(final String message) {
		System.out.println(message);
		
		this.index = 0;
		this.line = scanner.nextLine();
		
		return isExit(this.line);
	}
	
	public boolean isExit(final String line) {
		if ("exit".equals(line))
			this.exitFlag = true;
		
		return this.exitFlag;
	}
	
	public boolean hasFailedMoreThanTwo() {
		return this.failedCount > 2;
	}
	
	public void printError(final String message) {
		System.out.println("Error: "+message);
	}
	
	public ArrayList<Token> run() {
		this.failedCount = 0;
		this.numberCount = 0;
		this.exitFlag = false;
		this.isNegative = false;
		final ArrayList<Token> tokens = new ArrayList<>();
		
		char character;
		Token token;

		for (this.index = 0; this.index < this.line.length(); this.index++) {
			eatWhiteSpace();
			
			character = peek();
			if (character == '\0')
				break;

			if (character == '-' && (Character.isDigit(peek(1)) || peek(1) == '(')) {
				tokens.add(new Token(TokenType.LEFT_PAREN));
				tokens.add(new Token(TokenType.NEGATE));
				this.isNegative = true;
				continue;
			} else if (isOperator(character) && isOperator(peek(1))) {
				printError("You typed a invalid expression.");
				return null;
			}
				
			token = VALUES.get(character);
			token = token != null ? token : parseNumber(this.line);
			
			if (token == null || exitFlag) {
				return null;
			}
			
			tokens.add(token);
			
			if (this.isNegative && this.numberCount > 1) {
				this.isNegative = false;
				this.numberCount = 0;
				tokens.add(new Token(TokenType.RIGHT_PAREN));
			}
		}
		
		if (tokens.isEmpty())
			return null;
		
		return tokens;
	}
	
	public Token handleNumberException(final String temp) {
		
		if (hasFailedMoreThanTwo()) {
			return null;
		}
		
		this.failedCount++;
		
		Token token = null;
		
		int oldIndex = this.index;
		String oldLine = this.line;
		
		if (scanLine("Input '"+temp+"' was not a number. Please type a number and hit enter.")) {
			return null;
		}
		
		token = parseNumber(this.line);
		
		this.index = oldIndex;
		this.line = oldLine;

		return token;
	}
	
	private void eatWhiteSpace() {
		while (Character.isWhitespace(peek()))
			advance();
	}
	
	private boolean isAlphaNumber(char value) {
		return Character.isDigit(value) || Character.isLetter(value);
	}
	
	private boolean isOperator(char value) {
		if (value == '(' || value == ')' || value == '-')
			return false;
		return VALUES.get(value) != null;
	}
	
	private boolean isAtEnd() {
		return this.index >= this.line.length();
	}
	 
	private char peek() {
		if (isAtEnd())
			return '\0';
		return this.line.charAt(index);
	}
	
	private char peek(final int inc) {
		final int newIndex = this.index + inc;
		if (isAtEnd() || (newIndex >= this.line.length()))
			return '\0';
		return this.line.charAt(newIndex);
	}
	
	private char advance() {
		this.index++;
		return this.line.charAt(index-1);
	}
	
	private Token parseNumber(final String currentLine) {
		if (isExit(currentLine))
			return null;
		
		final int start = this.index;
		
		while (isAlphaNumber(peek())) {
			advance();
		
			if (peek() == '.') {
				advance();
			}
		}
		
		final String temp = currentLine.substring(start, this.index);
		double value = 0;
		
		try {
			value = Double.parseDouble(temp);
		} catch(final NumberFormatException exception) {
			return handleNumberException(temp);
		}
		
		this.index--;
		this.numberCount++;
		return new Token(TokenType.NUMBER, value);
	}
}