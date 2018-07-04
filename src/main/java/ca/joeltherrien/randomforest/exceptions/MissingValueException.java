package ca.joeltherrien.randomforest.exceptions;

import ca.joeltherrien.randomforest.Covariate;
import ca.joeltherrien.randomforest.CovariateRow;

public class MissingValueException extends RuntimeException{

	/**
	 * 
	 */
	private static final long serialVersionUID = 6808060079431207726L;
	
	public MissingValueException(CovariateRow row, Covariate.SplitRule rule) {
		super("Missing value at CovariateRow " + row + rule);
	}

}
