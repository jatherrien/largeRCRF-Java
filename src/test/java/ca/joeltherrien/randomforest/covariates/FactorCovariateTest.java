package ca.joeltherrien.randomforest.covariates;


import ca.joeltherrien.randomforest.utils.Utils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.Collection;
import java.util.List;

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

        final Collection<FactorCovariate.FactorSplitRule> splitRules = petCovariate.generateSplitRules(null, 100);

        assertEquals(splitRules.size(), 3);

        // TODO verify the contents of the split rules

    }


    private FactorCovariate createTestCovariate(){
        final List<String> levels = Utils.easyList("DOG", "CAT", "MOUSE");

        return new FactorCovariate("pet", levels);
    }


}
