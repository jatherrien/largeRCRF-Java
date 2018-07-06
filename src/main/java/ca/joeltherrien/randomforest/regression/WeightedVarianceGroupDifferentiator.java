package ca.joeltherrien.randomforest.regression;

import ca.joeltherrien.randomforest.tree.GroupDifferentiator;

import java.util.List;

public class WeightedVarianceGroupDifferentiator implements GroupDifferentiator<Double> {

    static{
        GroupDifferentiator.registerGroupDifferentiator("WeightedVarianceGroupDifferentiator", new WeightedVarianceGroupDifferentiator());
    }

    @Override
    public Double differentiate(List<Double> leftHand, List<Double> rightHand) {

        final double leftHandSize = leftHand.size();
        final double rightHandSize = rightHand.size();
        final double n = leftHandSize + rightHandSize;

        if(leftHandSize == 0 || rightHandSize == 0){
            return null;
        }

        final double leftHandMean = leftHand.stream().mapToDouble(db -> db/leftHandSize).sum();
        final double rightHandMean = rightHand.stream().mapToDouble(db -> db/rightHandSize).sum();

        final double leftVariance = leftHand.stream().mapToDouble(db -> (db - leftHandMean)*(db - leftHandMean)).sum();
        final double rightVariance = rightHand.stream().mapToDouble(db -> (db - rightHandMean)*(db - rightHandMean)).sum();

        return -(leftVariance + rightVariance) / n;

    }

}
