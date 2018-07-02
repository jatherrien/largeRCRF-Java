package ca.joeltherrien.randomforest;

import lombok.Data;

import java.util.List;

/**
 * Very simple class that contains two lists; it's essentially a tuple.
 * 
 * @author joel
 *
 */
@Data
public class Split<Y> {

	public final List<Row<Y>> leftHand;
	public final List<Row<Y>> rightHand;

}
