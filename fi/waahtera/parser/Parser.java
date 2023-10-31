package fi.waahtera.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import fi.waahtera.math.Operator;

public class Parser {
	
	private static final HashMap<String, String> EXPRESSION_CACHE = new HashMap<>();
	private static final HashMap<TokenType, Operator> OPERATORS = new HashMap<>();
	
	private static final Deque<Double> VALUE_STACK = new ArrayDeque<>();
	
	private static final ArrayList<Token> TOKENS = new ArrayList<>();
	
	private static final List<TokenType> PRIMARY = Arrays.asList(TokenType.PLUS, TokenType.MINUS);
	private static final List<TokenType> TERNARY = Arrays.asList(TokenType.MUL, TokenType.DIV, TokenType.MOD);
	private static final List<TokenType> EXP = Arrays.asList(TokenType.POW);
	
	private static final Path EXPRESSION_DIRECTORY = Paths.get("expressions");
	private static final String CALL_EXCEPTION_MSG = "Expression call exception";
	
	private final Lexer lexer;
	private String extra;
	private boolean exitParser;
	private TokenType type;
	private Token token;
	private int index;

	public Parser(Lexer lexer) {
		this.lexer = lexer;
		this.extra = null;

		OPERATORS.put(TokenType.PLUS, new Operator(TokenType.PLUS, 
				(firstNumber, secondNumber) -> secondNumber + firstNumber));
		OPERATORS.put(TokenType.MINUS, new Operator(TokenType.MINUS,
				(firstNumber, secondNumber) -> secondNumber - firstNumber));
		OPERATORS.put(TokenType.MUL, new Operator(TokenType.MUL,
				(firstNumber, secondNumber) -> secondNumber * firstNumber));
		OPERATORS.put(TokenType.DIV, new Operator(TokenType.DIV,
				(firstNumber, secondNumber) -> secondNumber / firstNumber));
		OPERATORS.put(TokenType.MOD, new Operator(TokenType.MOD,
				(firstNumber, secondNumber) -> secondNumber % firstNumber));
		OPERATORS.put(TokenType.POW, new Operator(TokenType.POW,
				(firstNumber, secondNumber) -> Math.pow(secondNumber, firstNumber)));
		
	}
	
	private void printException(final String message, final Exception exception) {
		lexer.printError(message+": "+exception.getMessage());
	}
	
	private void printMissing(final String missing) {
		lexer.printError(missing+" was missing from a expression");
	}
	
	private TokenType peekType() {
		if (this.index >= TOKENS.size())
			return null;
		
		this.token = TOKENS.get(index);
		if (this.token != null)
			this.type = this.token.getType();
		
		return this.type;
	}
	
	private void handleOperator(final List<TokenType> list, final Runnable func, TokenType op) {
		if (this.exitParser)
			return;
		
		while (list.contains(op)) {
			this.index++;
			
			try {
				func.run();
			} catch (final Exception exception) {
				printException(CALL_EXCEPTION_MSG, exception);
			}
			
			if (this.exitParser)
				return;
			
			if (this.extra != null)
				System.out.print(this.extra);

			VALUE_STACK.push(OPERATORS.get(op).eval(VALUE_STACK.pop(), VALUE_STACK.pop()));

			op = peekType();
		}
		
		return;
	}
	
	private void value() {
		if (this.exitParser)
			return;
		
		peekType();

		switch (type) {
		case NUMBER:
			this.index++;
			VALUE_STACK.push(token.getValue());
			break;
		case LEFT_PAREN:
			this.index++;
			runExpression();
			peekType();
			
			if (type != TokenType.RIGHT_PAREN)
				printMissing("Right paren ')'");
			else
				index++;
			
			break;
		case RIGHT_PAREN:
			printMissing("Left paren '('");
			break;
		case NEGATE:
			this.index++;
			runExpression();
			VALUE_STACK.push(-VALUE_STACK.pop());
			System.out.println("Negate = "+VALUE_STACK.peek());
			break;
		default:
			Token token = lexer.handleNumberException(type.toString());
			
			if (token == null) {
				this.exitParser = true;
				return;
			}
			
			TOKENS.add(token);
			value();
			return;
		}
	}
	
	private void expression(final List<TokenType> list, final Runnable func) {
		try {
			func.run();
		} catch (final Exception exception) {
			printException(CALL_EXCEPTION_MSG, exception);
		}
		
		handleOperator(list, func, peekType());
	}
	
	private void runExpression() {
		expression(PRIMARY, () -> 
			expression(TERNARY, () -> 
				expression(EXP, () -> value())));
	}
	
	public void eval(final ArrayList<Token> tokens) {
		this.index = 0;
		this.exitParser = false;
		TOKENS.clear();
		VALUE_STACK.clear();
		
		if (lexer.hasFailedMoreThanTwo() || tokens == null)
			return;
		
		TOKENS.addAll(tokens);
		runExpression();
	}
	
	public void eval(final String extra, final String line) {
		this.extra = extra;
		lexer.setLine(line);
		eval(lexer.run());
		this.extra = null;
	}
	
	private String tokensToString(final ArrayList<Token> tokens) {
		final StringBuilder builder = new StringBuilder();

		tokens.forEach((token) -> {
			if (token == null)
				return;
			
			TokenType type = token.getType();
			
			if (type != TokenType.NUMBER)
				builder.append(type);
			else
				builder.append(token.getValue());
		});
		
		return builder.toString();
	}
	
	private void createExprDirIfNotExists() {
		if (!Files.exists(EXPRESSION_DIRECTORY)) {
			try {
				Files.createDirectory(EXPRESSION_DIRECTORY);
			} catch (final IOException exception) {
				printException("Directory expressions error", exception);
			}
		}
	}
	
	private File isFileNameValid(final String line) {
		if (line.isBlank() || line.length() < 5) {
			lexer.printError("You are missing a FileName from yours input.");
			return null;
		}
		
		createExprDirIfNotExists();
		
		return new File(EXPRESSION_DIRECTORY.toString(), line.substring(5)+".expr");
	}
	
	private ArrayList<Token> loadNoRun(final File file) {
		
		try {
			final ObjectInputStream objectInput = new ObjectInputStream(new FileInputStream(file));
			ArrayList<Token> tokens = (ArrayList<Token>) objectInput.readObject();
			objectInput.close();
			return tokens;
		} catch (final IOException | ClassNotFoundException exception) {
			printException("File "+file.getName()+" error", exception);
		}
		
		return null;
	}
	
	private String getExpressionFromCache(final Path path) {
		final Path fileNamePath = path.getFileName();
		
		if (fileNamePath == null)
			return "";
		
		final String fileName = fileNamePath.toString();
		String expr = EXPRESSION_CACHE.get(fileName);
		
		if (expr == null) {
			expr = tokensToString(loadNoRun(path.toFile()));
			EXPRESSION_CACHE.put(fileName, expr);
		}
		
		return expr;
	}
	
	public void load(final String line) {
		final File file = isFileNameValid(line);
		if (file == null) 
			return;
		
		if (!file.exists() || !file.canRead()) {
			lexer.printError("File "+file.getName()+" does not exists.");
			return;
		}
		
		lexer.setFailedCount(0);
		ArrayList<Token> tokens = loadNoRun(file);
		eval(tokens);
		
		System.out.println("Loaded expression tokens from a file: "+file.getName()+"\n");
	}
	
	public void save(final String line) {
		if (TOKENS.isEmpty()) {
			lexer.printError("There was no last expression to save.");
			return;
		}
		
		final File file = isFileNameValid(line);
		if (file == null) 
			return;
		
		try {
			if (!file.exists()) {
				try {
					file.createNewFile();
				} catch (final IOException exception) {
					printException("Failed to create a file due to", exception);
				}
			} else {
				while (!lexer.isExitFlag()) {
					lexer.scanLine("File "+file.getName()+" exists already. Type 'yes' if you want to overwrite it or a empty line if not.");
					final String reply = lexer.getLine();
			
					if ("yes".equals(reply)) {
						file.createNewFile();
						break;
					} else {
						return;
					}
				}
			}
		
			final ObjectOutputStream objectOutput = new ObjectOutputStream(new FileOutputStream(file));
			objectOutput.writeObject(TOKENS);
			objectOutput.flush();
			objectOutput.close();
			System.out.println("Saved last expression tokens to a file: "+file.getName()+"\n");
		} catch (final Exception exception) {
			printException("File "+file.getName()+" error", exception);
		}
	}
	
	public void list() {
		try {
			if (!Files.exists(EXPRESSION_DIRECTORY, LinkOption.NOFOLLOW_LINKS)) {
				createExprDirIfNotExists();
			}
		
			final DirectoryStream<Path> directory = Files.newDirectoryStream(EXPRESSION_DIRECTORY);
			final Iterator<Path> iterator = directory.iterator();
			
			if (!iterator.hasNext()) {
				lexer.printError("Directory '"+EXPRESSION_DIRECTORY.toString()+"' was empty of files.");
				return;
			}
			
			Path path;
			while (iterator.hasNext()) {
				path = iterator.next();
				System.out.println("File: "+path.getFileName() + ", expression: "+getExpressionFromCache(path));
			}
				
			directory.close();
		} catch (final IOException exception) {
			printException("expressions directory", exception);
		}
	}
}
