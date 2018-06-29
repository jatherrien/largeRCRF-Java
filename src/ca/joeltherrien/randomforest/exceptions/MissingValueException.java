package ca.joeltherrien.randomforest.exceptions;

import ca.joeltherrien.randomforest.Row;
import ca.joeltherrien.randomforest.SplitRule;

public class MissingValueException extends RuntimeException{

	/**
	 * 
	 */
	private static final long serialVersionUID = 6808060079431207726L;
	
	public MissingValueException(Row<?> row, SplitRule rule) {
		super("Missing value at row " + row + rule);
	}

}
