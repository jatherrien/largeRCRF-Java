package ca.joeltherrien.randomforest;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

public abstract class SplitRule implements Serializable {

    /**
     * Applies the SplitRule to a list of rows and returns a Split object, which contains two lists for both sides.
     * This method is primarily used during the training of a tree when splits are being tested.
     *
     * @param rows
     * @param <Y>
     * @return
     */
    public <Y> Split<Y> applyRule(List<Row<Y>> rows) {
        final List<Row<Y>> leftHand = new LinkedList<>();
        final List<Row<Y>> rightHand = new LinkedList<>();

        for(final Row<Y> row : rows) {

            if(isLeftHand(row)){
                leftHand.add(row);
            }
            else{
                rightHand.add(row);
            }

        }

        return new Split<>(leftHand, rightHand);
    }

    public abstract boolean isLeftHand(CovariateRow row);
	
}
