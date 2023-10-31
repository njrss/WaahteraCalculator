package fi.waahtera.calculator;

import java.util.HashMap;
import java.util.Scanner;
import java.util.function.Consumer;

import fi.waahtera.parser.Lexer;
import fi.waahtera.parser.Parser;

public class WaahteraCalculator {
	
	public static void main(String[] args) {
		final WaahteraCalculator calculator = new WaahteraCalculator();

		try {
			calculator.loop();
		} catch (final Throwable throwable) {
			System.out.println("Error: " + throwable.getMessage());
		}
	}
	
	private void loop() {
		final Lexer lexer = new Lexer(new Scanner(System.in));
		final Parser parser = new Parser(lexer);
		
		System.out.println("Calculator made by njrss (c) 2023");
		System.out.println("- Supported operators: + - * / % ^");
		System.out.println("- Supported commands: save FileName | load FileName | list | exit");
		System.out.println("- .expr is inserted automatically to the end of FileName\n");
		
		String expr = "1.5 + 2 * 2.4 - 0.5 + 1.2 % 3 / 1.9 + 0.3";
		System.out.println("Running a test expression.\n"+expr+" = ");
		parser.eval("  ", expr);
		System.out.println("");
		
		final HashMap<String, Consumer<String>> parserFunc = new HashMap<>();
		parserFunc.put("save", expression -> parser.save(expression));
		parserFunc.put("load", expression -> parser.load(expression));
		parserFunc.put("list", expression -> parser.list());
		
		Consumer<String> call;
		
		while (!lexer.isExitFlag()) {
			lexer.scanLine("Please type a math expression and hit enter.");
			
			expr = lexer.getLine();
			
			if (expr != null && !expr.isBlank() && expr.length() > 3) {
				call = parserFunc.get(expr.substring(0, 4));
				
				if (call != null) {
					call.accept(expr);
					continue;
				}
			}
			
			parser.eval(lexer.run());
		}
	}
}