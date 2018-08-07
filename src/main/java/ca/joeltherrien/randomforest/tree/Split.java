package ca.joeltherrien.randomforest.tree;

import ca.joeltherrien.randomforest.Row;
import lombok.Data;

import java.util.List;

/**
 * Very simple class that contains three lists; it's essentially a thruple.
 * 
 * @author joel
 *
 */
@Data
public class Split<Y> {

	public final List<Row<Y>> leftHand;
	public final List<Row<Y>> rightHand;
	public final List<Row<Y>> naHand;

}
