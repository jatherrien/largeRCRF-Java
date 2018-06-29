package ca.joeltherrien.randomforest;

import java.util.List;

public interface SplitRule {

	<Y> Split<Y> applyRule(List<Row<Y>> rows);
	
}
