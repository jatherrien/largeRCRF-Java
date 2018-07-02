package ca.joeltherrien.randomforest.exceptions;

import ca.joeltherrien.randomforest.CovariateRow;
import ca.joeltherrien.randomforest.Row;
import ca.joeltherrien.randomforest.SplitRule;

public class MissingValueException extends RuntimeException{

	/**
	 * 
	 */
	private static final long serialVersionUID = 6808060079431207726L;
	
	public MissingValueException(CovariateRow row, SplitRule rule) {
		super("Missing value at CovariateRow " + row + rule);
	}

}
