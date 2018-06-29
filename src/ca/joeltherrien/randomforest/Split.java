package ca.joeltherrien.randomforest;

import java.util.List;

/**
 * Very simple class that contains two lists; it's essentially a tuple.
 * 
 * @author joel
 *
 */
public class Split<Y> {

	public final List<Row<Y>> leftHand;
	public final List<Row<Y>> rightHand;
	
	public Split(List<Row<Y>> leftHand, List<Row<Y>> rightHand){
		this.leftHand = leftHand;
		this.rightHand = rightHand;
	}
}
