package ca.joeltherrien.randomforest;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public interface Covariate<V> extends Serializable {

    String getName();

    Collection<? extends SplitRule<V>> generateSplitRules(final List<Value<V>> data, final int number);

    Value<V> createValue(V value);

    interface Value<V> extends Serializable{

        Covariate<V> getParent();

        V getValue();

    }

    interface SplitRule<V> extends Serializable{

        Covariate<V> getParent();

        /**
         * Applies the SplitRule to a list of rows and returns a Split object, which contains two lists for both sides.
         * This method is primarily used during the training of a tree when splits are being tested.
         *
         * @param rows
         * @param <Y>
         * @return
         */
        default <Y> Split<Y> applyRule(List<Row<Y>> rows) {
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

        boolean isLeftHand(CovariateRow row);
    }


}
