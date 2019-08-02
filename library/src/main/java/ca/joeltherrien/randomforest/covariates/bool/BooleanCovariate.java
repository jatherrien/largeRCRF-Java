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

package ca.joeltherrien.randomforest.covariates.bool;

import ca.joeltherrien.randomforest.Row;
import ca.joeltherrien.randomforest.covariates.Covariate;
import ca.joeltherrien.randomforest.tree.Split;
import ca.joeltherrien.randomforest.utils.SingletonIterator;
import lombok.Getter;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

public final class BooleanCovariate implements Covariate<Boolean> {

    private static final long serialVersionUID = 1L;

    @Getter
    private final String name;

    @Getter
    private final int index;

    private boolean hasNAs = false;

    private final BooleanSplitRule splitRule; // there's only one possible rule for BooleanCovariates.

    public BooleanCovariate(String name, int index){
        this.name = name;
        this.index = index;
        splitRule = new BooleanSplitRule(this);
    }

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
        return "BooleanCovariate(name=" + this.name + ", index=" + this.index + ", hasNAs=" + this.hasNAs + ")";
    }

    public class BooleanValue implements Value<Boolean>{

        private static final long serialVersionUID = 1L;

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


}
