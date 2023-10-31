package fi.waahtera.parser;

import java.io.Serializable;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
public class Token implements Serializable {
	private static final long serialVersionUID = -766475967504744102L;
	
	@Getter
	private final TokenType type;
	@Setter
	@Getter
	private double value;
	
	public Token(TokenType type, double value) {
		this.type = type;
		this.value = value;
	}
}
