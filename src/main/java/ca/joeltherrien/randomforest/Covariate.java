package ca.joeltherrien.randomforest;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public interface Covariate<V> extends Serializable {

    String getName();

    Collection<? extends SplitRule<V>> generateSplitRules(final List<Value<V>> data, final int number);

    Value<V> createValue(V value);

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

            final List<Boolean> nonMissingDecisions = new ArrayList<>();
            final List<Row<Y>> missingValueRows = new ArrayList<>();


            for(final Row<Y> row : rows) {
                final Value<V> value = (Value<V>) row.getCovariateValue(getParent().getName());

                if(value.isNA()){
                    missingValueRows.add(row);
                    continue;
                }

                final boolean isLeftHand = isLeftHand(value);
                nonMissingDecisions.add(isLeftHand);

                if(isLeftHand){
                    leftHand.add(row);
                }
                else{
                    rightHand.add(row);
                }

            }

            if(nonMissingDecisions.size() == 0 && missingValueRows.size() > 0){
                throw new IllegalArgumentException("Can't apply " + this + " when there are rows with missing data and no non-missing value rows");
            }

            final Random random = ThreadLocalRandom.current();
            for(final Row<Y> missingValueRow : missingValueRows){
                final boolean randomDecision = nonMissingDecisions.get(random.nextInt(nonMissingDecisions.size()));

                if(randomDecision){
                    leftHand.add(missingValueRow);
                }
                else{
                    rightHand.add(missingValueRow);
                }
            }

            return new Split<>(leftHand, rightHand);
        }

        default boolean isLeftHand(CovariateRow row){
            final Value<V> value = (Value<V>) row.getCovariateValue(getParent().getName());
            return isLeftHand(value);
        }

        boolean isLeftHand(Value<V> value);
    }


}
