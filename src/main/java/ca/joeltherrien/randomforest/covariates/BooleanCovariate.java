/*
 * Copyright (c) 2019 Joel Therrien.
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ca.joeltherrien.randomforest.covariates;

import ca.joeltherrien.randomforest.Row;
import ca.joeltherrien.randomforest.tree.Split;
import ca.joeltherrien.randomforest.utils.SingletonIterator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.*;

@RequiredArgsConstructor
public final class BooleanCovariate implements Covariate<Boolean> {

    @Getter
    private final String name;

    @Getter
    private final int index;

    private boolean hasNAs = false;

    private final BooleanSplitRule splitRule = new BooleanSplitRule(); // there's only one possible rule for BooleanCovariates.

    @Override
    public <Y> Iterator<Split<Y, Boolean>> generateSplitRuleUpdater(List<Row<Y>> data, int number, Random random) {
        return new SingletonIterator<>(this.splitRule.applyRule(data));
    }

    @Override
    public BooleanValue createValue(Boolean value) {
        return new BooleanValue(value);
    }

    @Override
    public Value<Boolean> createValue(String value) {
        if(value == null || value.equalsIgnoreCase("na")){
            hasNAs = true;
            return createValue( (Boolean) null);
        }

        if(value.equalsIgnoreCase("true")){
            return createValue(true);
        }
        else if(value.equalsIgnoreCase("false")){
            return createValue(false);
        }
        else{
            throw new IllegalArgumentException("Require either true/false/na to create BooleanCovariate");
        }
    }

    @Override
    public boolean hasNAs() {
        return hasNAs;
    }

    @Override
    public String toString(){
        return "BooleanCovariate(name=" + name + ")";
    }

    public class BooleanValue implements Value<Boolean>{

        private final Boolean value;

        private BooleanValue(final Boolean value){
            this.value = value;
        }

        @Override
        public BooleanCovariate getParent() {
            return BooleanCovariate.this;
        }

        @Override
        public Boolean getValue() {
            return value;
        }

        @Override
        public boolean isNA() {
            return value == null;
        }
    }


    public class BooleanSplitRule implements SplitRule<Boolean>{

        @Override
        public final String toString() {
            return "BooleanSplitRule";
        }

        @Override
        public BooleanCovariate getParent() {
            return BooleanCovariate.this;
        }

        @Override
        public boolean isLeftHand(final Value<Boolean> value) {
            if(value.isNA()) {
                throw new IllegalArgumentException("Trying to determine split on missing value");
            }

            return !value.getValue();
        }
    }
}
