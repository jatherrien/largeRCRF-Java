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

package ca.joeltherrien.randomforest.covariates.factor;

import ca.joeltherrien.randomforest.Row;
import ca.joeltherrien.randomforest.covariates.Covariate;
import ca.joeltherrien.randomforest.tree.Split;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.*;
import java.util.stream.Collectors;

public final class FactorCovariate implements Covariate<String> {

    private static final long serialVersionUID = 1L;

    @Getter
    private final String name;

    @Getter
    private final int index;

    private final Map<String, FactorValue> factorLevels;
    private final FactorValue naValue;
    private final int numberOfPossiblePairings;

    private boolean hasNAs;

    private final boolean haveNASplitPenalty;
    @Override
    public boolean haveNASplitPenalty(){
        // penalty would add worthless computational time if there are no NAs
        return hasNAs && haveNASplitPenalty;
    }


    public FactorCovariate(final String name, final int index, List<String> levels, final boolean haveNASplitPenalty){
        this.name = name;
        this.index = index;
        this.factorLevels = new HashMap<>();

        for(final String level : levels){
            if(level.equalsIgnoreCase("na")){
                throw new IllegalArgumentException("Cannot use NA (case-insensitive) as a level in factor covariate " + name);
            }

            final FactorValue newValue = new FactorValue(level);

            factorLevels.put(level, newValue);
        }

        int numberOfPossiblePairingsTemp = 1;
        for(int i=0; i<levels.size()-1; i++){
            numberOfPossiblePairingsTemp *= 2;
        }
        this.numberOfPossiblePairings = numberOfPossiblePairingsTemp-1;

        this.naValue = new FactorValue(null);

        this.haveNASplitPenalty = haveNASplitPenalty;
    }



    @Override
    public <Y> Iterator<Split<Y, String>> generateSplitRuleUpdater(List<Row<Y>> data, int number, Random random) {
        if(hasNAs()){
            data = data.stream().filter(row -> !row.getCovariateValue(this).isNA()).collect(Collectors.toList());
        }

        if(number == 0){ // nsplit = 0 => try every possibility, although we limit it to the number of observations.
            number = data.size();
        }

        final Set<Split<Y, String>> splits = new HashSet<>();

        // This is to ensure we don't get stuck in an infinite loop for small factors
        number = Math.min(number, numberOfPossiblePairings);
        final List<FactorValue> levels = new ArrayList<>(factorLevels.values());

        while(splits.size() < number){
            Collections.shuffle(levels, random);
            final Set<String> leftSideValues = new HashSet<>();
            leftSideValues.add(levels.get(0).getValue());

            for(int i=1; i<levels.size()/2; i++){
                if(random.nextBoolean()){
                    leftSideValues.add(levels.get(i).getValue());
                }
            }

            splits.add(new FactorSplitRule(this, leftSideValues).applyRule(data));
        }

        return splits.iterator();

    }


    @Override
    public FactorValue createValue(String value) {
        if(value == null || value.equalsIgnoreCase("na")){
            this.hasNAs = true;
            return this.naValue;
        }

        final FactorValue factorValue = factorLevels.get(value);

        if(factorValue == null){
            throw new IllegalArgumentException(value + " is not a level in FactorCovariate " + name);
        }

        return factorValue;
    }


    @Override
    public boolean hasNAs() {
        return hasNAs;
    }

    @Override
    public String toString() {
        return "FactorCovariate(name=" + this.name + ", index=" + this.index + ", hasNAs=" + this.hasNAs + ")";
    }

    @EqualsAndHashCode
    public final class FactorValue implements Covariate.Value<String>{

        private static final long serialVersionUID = 1L;

        private final String value;

        private FactorValue(final String value){
            this.value = value;
        }

        @Override
        public FactorCovariate getParent() {
            return FactorCovariate.this;
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public boolean isNA() {
            return value == null;
        }
    }

}
