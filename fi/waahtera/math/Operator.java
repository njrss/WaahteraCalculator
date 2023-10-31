package fi.waahtera.math;

import java.util.function.BiFunction;

import fi.waahtera.parser.TokenType;

public class Operator implements BiFunction<Double, Double, Double> {

	private final String symbol;
	private final BiFunction<Double, Double, Double> operation;

	public Operator(TokenType type, BiFunction<Double, Double, Double> operation) {
		this.symbol = type.getSymbol();
		this.operation = operation;
	}

	@Override
	public Double apply(final Double leftHandNumber, final Double rightHandNumber) {
		return this.operation.apply(leftHandNumber, rightHandNumber);
	}

	public Double eval(final Double numberOne, final Double numberTwo) {
		final Double result = this.operation.apply(numberOne, numberTwo);

		System.out.println(numberTwo + " " + this.symbol + " " + numberOne + " = " + result);
		return result;
	}
}
