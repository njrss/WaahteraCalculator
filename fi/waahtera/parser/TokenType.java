package fi.waahtera.parser;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum TokenType {
	PLUS("+"),
	MINUS("-"),
	NEGATE("Negate"),
	MOD("%"),
	MUL("*"),
	DIV("/"),
	POW("^"),
	LEFT_PAREN("("),
	RIGHT_PAREN(")"),
	NUMBER("Number");
	
	@Getter
	private final String symbol;
	
	@Override
	public String toString() {
		return this.symbol;
	}
}
