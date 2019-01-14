package ca.joeltherrien.randomforest.tree;

import ca.joeltherrien.randomforest.Row;
import ca.joeltherrien.randomforest.covariates.Covariate;
import lombok.Data;

import java.util.List;

/**
 * Very simple class that contains three lists and a SplitRule.
 * 
 * @author joel
 *
 */
@Data
public final class Split<Y, V> {

	public final Covariate.SplitRule<V> splitRule;
	public final List<Row<Y>> leftHand;
	public final List<Row<Y>> rightHand;
	public final List<Row<Y>> naHand;

}
