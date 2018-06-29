package ca.joeltherrien.randomforest;

import java.util.LinkedList;
import java.util.List;

import ca.joeltherrien.randomforest.exceptions.MissingValueException;

public class NumericSplitRule implements SplitRule{

	public final String covariateName;	
	public final double threshold;
	
	public NumericSplitRule(String covariateName, double threshold) {
		super();
		this.covariateName = covariateName;
		this.threshold = threshold;
	}
	
	@Override
	public final String toString() {
		return "NumericSplitRule on " + covariateName + " at " + threshold;
	}

	@Override
	public <Y> Split<Y> applyRule(List<Row<Y>> rows) {
		final List<Row<Y>> leftHand = new LinkedList<>();
		final List<Row<Y>> rightHand = new LinkedList<>();
		
		for(final Row<Y> row : rows) {
			final Value x = row.getCovariate(covariateName);
			if(x == null) {
				throw new MissingValueException(row, this);
			}
			
			final NumericValue xNum = (NumericValue) x;
			
		}
		
		// TODO Auto-generated method stub
		return null;
	}
	
	
}
