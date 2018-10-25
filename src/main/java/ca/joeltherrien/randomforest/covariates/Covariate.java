package ca.joeltherrien.randomforest.covariates;

import ca.joeltherrien.randomforest.CovariateRow;
import ca.joeltherrien.randomforest.Row;
import ca.joeltherrien.randomforest.tree.Split;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public interface Covariate<V> extends Serializable {

    String getName();

    int getIndex();

    Collection<? extends SplitRule<V>> generateSplitRules(final List<Value<V>> data, final int number);

    Value<V> createValue(V value);

    /**
     * Creates a Value of the appropriate type from a String; primarily used when parsing CSVs.
     *
     * @param value
     * @return
     */
    Value<V> createValue(String value);

    interface Value<V> extends Serializable{

        Covariate<V> getParent();

        V getValue();

        boolean isNA();

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

            final List<Row<Y>> missingValueRows = new ArrayList<>();


            for(final Row<Y> row : rows) {
                final Value<V> value = (Value<V>) row.getCovariateValue(getParent());

                if(value.isNA()){
                    missingValueRows.add(row);
                    continue;
                }

                final boolean isLeftHand = isLeftHand(value);
                if(isLeftHand){
                    leftHand.add(row);
                }
                else{
                    rightHand.add(row);
                }

            }


            return new Split<>(leftHand, rightHand, missingValueRows);
        }

        default boolean isLeftHand(CovariateRow row, final double probabilityNaLeftHand){
            final Value<V> value = (Value<V>) row.getCovariateValue(getParent());

            if(value.isNA()){
                return ThreadLocalRandom.current().nextDouble() <= probabilityNaLeftHand;
            }

            return isLeftHand(value);
        }

        boolean isLeftHand(Value<V> value);
    }


}
