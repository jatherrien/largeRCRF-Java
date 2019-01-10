package ca.joeltherrien.randomforest.covariates;

import ca.joeltherrien.randomforest.CovariateRow;
import ca.joeltherrien.randomforest.Row;
import ca.joeltherrien.randomforest.tree.Split;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public interface Covariate<V> extends Serializable {

    String getName();

    int getIndex();

    <Y> Iterator<Split<Y, V>> generateSplitRuleUpdater(final List<Row<Y>> data, final int number, final Random random);

    Value<V> createValue(V value);

    /**
     * Creates a Value of the appropriate type from a String; primarily used when parsing CSVs.
     *
     * @param value
     * @return
     */
    Value<V> createValue(String value);

    boolean hasNAs();

    interface Value<V> extends Serializable{

        Covariate<V> getParent();

        V getValue();

        boolean isNA();

    }

    interface SplitRuleUpdater<Y, V> extends Iterator<Split<Y, V>>{
        Split<Y, V> currentSplit();
        SplitUpdate<Y, V> nextUpdate();
    }

    interface SplitUpdate<Y, V> {
        SplitRule<V> getSplitRule();
        Collection<Row<Y>> rowsMovedToLeftHand();
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
        default <Y> Split<Y, V> applyRule(List<Row<Y>> rows) {
            final List<Row<Y>> leftHand = new LinkedList<>();
            final List<Row<Y>> rightHand = new LinkedList<>();

            final List<Row<Y>> missingValueRows = new ArrayList<>();


            for(final Row<Y> row : rows) {
                final Value<V> value = row.getCovariateValue(getParent());

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


            return new Split<>(this, leftHand, rightHand, missingValueRows);
        }

        default boolean isLeftHand(CovariateRow row, final double probabilityNaLeftHand){
            final Value<V> value = row.getCovariateValue(getParent());

            if(value.isNA()){
                return ThreadLocalRandom.current().nextDouble() <= probabilityNaLeftHand;
            }

            return isLeftHand(value);
        }

        boolean isLeftHand(Value<V> value);
    }


}
