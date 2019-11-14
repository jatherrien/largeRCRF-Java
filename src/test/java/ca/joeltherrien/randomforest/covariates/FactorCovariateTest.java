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
import ca.joeltherrien.randomforest.covariates.factor.FactorCovariate;
import ca.joeltherrien.randomforest.tree.Split;
import ca.joeltherrien.randomforest.utils.Utils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class FactorCovariateTest {

    @Test
    public void testEqualLevels() {
        final FactorCovariate petCovariate = createTestCovariate();

        final FactorCovariate.FactorValue dog1 = petCovariate.createValue("DOG");
        final FactorCovariate.FactorValue dog2 = petCovariate.createValue("DO" + "G");

        assertSame(dog1, dog2);

        final FactorCovariate.FactorValue cat1 = petCovariate.createValue("CAT");
        final FactorCovariate.FactorValue cat2 = petCovariate.createValue("CA" + "T");

        assertSame(cat1, cat2);

        final FactorCovariate.FactorValue mouse1 = petCovariate.createValue("MOUSE");
        final FactorCovariate.FactorValue mouse2 = petCovariate.createValue("MOUS" + "E");

        assertSame(mouse1, mouse2);


    }

    @Test
    public void testBadLevelException(){
        final FactorCovariate petCovariate = createTestCovariate();
        final Executable badCode = () -> petCovariate.createValue("vulcan");

        assertThrows(IllegalArgumentException.class, badCode, "vulcan is not a level in FactorCovariate pet");
    }

    @Test
    public void testAllSubsets(){
        final int n = 2*3; // ensure that n is a multiple of 3 for the test
        final FactorCovariate petCovariate = createTestCovariate();
        final List<Row<Double>> data = generateSampleData(petCovariate, n);

        final List<Split<Double, String>> splits = new ArrayList<>();

        petCovariate.generateSplitRuleUpdater(data, 100, new Random())
                .forEachRemaining(split -> splits.add(split));

        assertEquals(splits.size(), 3);

        // These are the 3 possibilities
        boolean dog_catmouse = false;
        boolean cat_dogmouse = false;
        boolean mouse_dogcat = false;

        for(Split<Double, String> split : splits){
            List<Row<Double>> smallerHand;
            List<Row<Double>> largerHand;

            if(split.getLeftHand().size() < split.getRightHand().size()){
                smallerHand = split.getLeftHand();
                largerHand = split.getRightHand();
            } else{
                smallerHand = split.getRightHand();
                largerHand = split.getLeftHand();
            }

            // There should be exactly one distinct value in the smaller list
            assertEquals(n/3, smallerHand.size());
            assertEquals(1,
                    smallerHand.stream()
                            .map(row -> row.getCovariateValue(petCovariate).getValue())
                            .distinct()
                            .count()
            );

            // There should be exactly two distinct values in the smaller list
            assertEquals(2*n/3, largerHand.size());
            assertEquals(2,
                    largerHand.stream()
                            .map(row -> row.getCovariateValue(petCovariate).getValue())
                            .distinct()
                            .count()
            );

            switch(smallerHand.get(0).getCovariateValue(petCovariate).getValue()){
                case "DOG":
                    dog_catmouse = true;
                case "CAT":
                    cat_dogmouse = true;
                case "MOUSE":
                    mouse_dogcat = true;
            }

        }

        assertTrue(dog_catmouse);
        assertTrue(cat_dogmouse);
        assertTrue(mouse_dogcat);

    }

    /*
     * There was a bug where if number==0 in generateSplitRuleUpdater, then the result was empty.
     */
    @Test
    public void testNumber0Subsets(){
        final int n = 2*3; // ensure that n is a multiple of 3 for the test
        final FactorCovariate petCovariate = createTestCovariate();
        final List<Row<Double>> data = generateSampleData(petCovariate, n);

        final List<Split<Double, String>> splits = new ArrayList<>();

        petCovariate.generateSplitRuleUpdater(data, 0, new Random())
                .forEachRemaining(split -> splits.add(split));

        assertEquals(splits.size(), 3);

        // These are the 3 possibilities
        boolean dog_catmouse = false;
        boolean cat_dogmouse = false;
        boolean mouse_dogcat = false;

        for(Split<Double, String> split : splits){
            List<Row<Double>> smallerHand;
            List<Row<Double>> largerHand;

            if(split.getLeftHand().size() < split.getRightHand().size()){
                smallerHand = split.getLeftHand();
                largerHand = split.getRightHand();
            } else{
                smallerHand = split.getRightHand();
                largerHand = split.getLeftHand();
            }

            // There should be exactly one distinct value in the smaller list
            assertEquals(n/3, smallerHand.size());
            assertEquals(1,
                    smallerHand.stream()
                    .map(row -> row.getCovariateValue(petCovariate).getValue())
                    .distinct()
                    .count()
            );

            // There should be exactly two distinct values in the smaller list
            assertEquals(2*n/3, largerHand.size());
            assertEquals(2,
                    largerHand.stream()
                            .map(row -> row.getCovariateValue(petCovariate).getValue())
                            .distinct()
                            .count()
            );

            switch(smallerHand.get(0).getCovariateValue(petCovariate).getValue()){
                case "DOG":
                    dog_catmouse = true;
                case "CAT":
                    cat_dogmouse = true;
                case "MOUSE":
                    mouse_dogcat = true;
            }

        }

        assertTrue(dog_catmouse);
        assertTrue(cat_dogmouse);
        assertTrue(mouse_dogcat);

    }

    @Test
    public void testSpitRuleUpdaterWithNAs(){
        // When some NAs were present calling generateSplitRuleUpdater caused an exception.

        final FactorCovariate covariate = createTestCovariate();
        final List<Row<Double>> sampleData = generateSampleData(covariate, 10);
        sampleData.add(Row.createSimple(Utils.easyMap("pet", "NA"), Collections.singletonList(covariate), 11, 5.0));

        covariate.generateSplitRuleUpdater(sampleData, 0, new Random());

        // Test passes if no exception has occurred.
    }


    private FactorCovariate createTestCovariate(){
        final List<String> levels = Utils.easyList("DOG", "CAT", "MOUSE");

        return new FactorCovariate("pet", 0, levels, false);
    }

    private List<Row<Double>> generateSampleData(Covariate covariate, int n){
        final List<Covariate> covariateList = Collections.singletonList(covariate);
        final List<Row<Double>> dataList = new ArrayList<>(n);

        final String[] levels = new String[]{"DOG", "CAT", "MOUSE"};

        for(int i=0; i<n; i++){
            dataList.add(Row.createSimple(Utils.easyMap("pet", levels[i % 3]), covariateList, 1, 1.0));
        }

        return dataList;
    }


}
