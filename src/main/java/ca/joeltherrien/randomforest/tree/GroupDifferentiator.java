package ca.joeltherrien.randomforest.tree;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * When choosing an optimal node to split on, we choose the split that maximizes the difference between the two groups.
 * The GroupDifferentiator has one method that outputs a score to show how different groups are. The larger the score,
 * the greater the difference.
 *
 */
public interface GroupDifferentiator<Y> {

    Double differentiate(List<Y> leftHand, List<Y> rightHand);

    @FunctionalInterface
    interface GroupDifferentiatorConstructor<Y>{

        GroupDifferentiator<Y> construct(ObjectNode node);

    }

}
