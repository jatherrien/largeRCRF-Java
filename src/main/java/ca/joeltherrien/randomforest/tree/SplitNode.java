package ca.joeltherrien.randomforest.tree;

import ca.joeltherrien.randomforest.Covariate;
import ca.joeltherrien.randomforest.CovariateRow;
import lombok.Builder;

@Builder
public class SplitNode<Y> implements Node<Y> {

    private final Node<Y> leftHand;
    private final Node<Y> rightHand;
    private final Covariate.SplitRule splitRule;

    @Override
    public Y evaluate(CovariateRow row) {

        if(splitRule.isLeftHand(row)){
            return leftHand.evaluate(row);
        }
        else{
            return rightHand.evaluate(row);
        }

    }
}
