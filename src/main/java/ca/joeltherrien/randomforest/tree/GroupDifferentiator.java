package ca.joeltherrien.randomforest.tree;

import java.util.List;

/**
 * When choosing an optimal node to split on, we choose the split that maximizes the difference between the two groups.
 * The GroupDifferentiator has one method that outputs a score to show how different groups are. The larger the score,
 * the greater the difference.
 *
 */
public interface GroupDifferentiator<Y> {

    Double differentiate(List<Y> leftHand, List<Y> rightHand);

}
