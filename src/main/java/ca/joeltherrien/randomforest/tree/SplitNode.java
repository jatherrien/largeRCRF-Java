package ca.joeltherrien.randomforest.tree;

import ca.joeltherrien.randomforest.covariates.Covariate;
import ca.joeltherrien.randomforest.CovariateRow;
import lombok.Builder;

@Builder
public class SplitNode<Y> implements Node<Y> {

    private final Node<Y> leftHand;
    private final Node<Y> rightHand;
    private final Covariate.SplitRule splitRule;
    private final double probabilityNaLeftHand; // used when assigning NA values

    @Override
    public Y evaluate(CovariateRow row) {

        if(splitRule.isLeftHand(row, probabilityNaLeftHand)){
            return leftHand.evaluate(row);
        }
        else{
            return rightHand.evaluate(row);
        }

    }
}
