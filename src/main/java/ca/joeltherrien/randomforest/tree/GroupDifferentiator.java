package ca.joeltherrien.randomforest.tree;

import java.util.Iterator;

/**
 * When choosing an optimal node to split on, we choose the split that maximizes the difference between the two groups.
 * The GroupDifferentiator has one method that cycles through an iterator of Splits (FYI; check if the iterator is an
 * instance of Covariate.SplitRuleUpdater; in which case you get access to the rows that change between splits)
 *
 *  If you want to implement a very trivial GroupDifferentiator that just takes two Lists as arguments, try extending
 *  SimpleGroupDifferentiator.
 */
public interface GroupDifferentiator<Y> {

    <V> SplitAndScore<Y, V> differentiate(Iterator<Split<Y, V>> splitIterator);

}
