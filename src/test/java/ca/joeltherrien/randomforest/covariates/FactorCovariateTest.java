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


import ca.joeltherrien.randomforest.covariates.factor.FactorCovariate;
import ca.joeltherrien.randomforest.utils.Utils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class FactorCovariateTest {

    @Test
    void verifyEqualLevels() {
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
    void verifyBadLevelException(){
        final FactorCovariate petCovariate = createTestCovariate();
        final Executable badCode = () -> petCovariate.createValue("vulcan");

        assertThrows(IllegalArgumentException.class, badCode, "vulcan is not a level in FactorCovariate pet");
    }

    @Test
    void testAllSubsets(){
        final FactorCovariate petCovariate = createTestCovariate();

        final List<SplitRule<String>> splitRules = new ArrayList<>();

        petCovariate.generateSplitRuleUpdater(null, 100, new Random())
                .forEachRemaining(split -> splitRules.add(split.getSplitRule()));

        assertEquals(splitRules.size(), 3);

        // TODO verify the contents of the split rules

    }


    private FactorCovariate createTestCovariate(){
        final List<String> levels = Utils.easyList("DOG", "CAT", "MOUSE");

        return new FactorCovariate("pet", 0, levels);
    }


}
