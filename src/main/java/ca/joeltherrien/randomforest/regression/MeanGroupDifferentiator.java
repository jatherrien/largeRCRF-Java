package ca.joeltherrien.randomforest.regression;

import ca.joeltherrien.randomforest.tree.GroupDifferentiator;
import ca.joeltherrien.randomforest.tree.ResponseCombiner;

import java.util.List;

public class MeanGroupDifferentiator implements GroupDifferentiator<Double> {

    static{
        GroupDifferentiator.registerGroupDifferentiator("MeanGroupDifferentiator", new MeanGroupDifferentiator());
    }

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
