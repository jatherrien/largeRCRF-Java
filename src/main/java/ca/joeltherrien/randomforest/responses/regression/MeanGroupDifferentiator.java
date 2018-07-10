package ca.joeltherrien.randomforest.responses.regression;

import ca.joeltherrien.randomforest.tree.GroupDifferentiator;

import java.util.List;

public class MeanGroupDifferentiator implements GroupDifferentiator<Double> {

    @Override
    public Double differentiate(List<Double> leftHand, List<Double> rightHand) {

        double leftHandSize = leftHand.size();
        double rightHandSize = rightHand.size();

        if(leftHandSize == 0 || rightHandSize == 0){
            return null;
        }

        double leftHandMean = leftHand.stream().mapToDouble(db -> db/leftHandSize).sum();
        double rightHandMean = rightHand.stream().mapToDouble(db -> db/rightHandSize).sum();

        return Math.abs(leftHandMean - rightHandMean);

    }

}
