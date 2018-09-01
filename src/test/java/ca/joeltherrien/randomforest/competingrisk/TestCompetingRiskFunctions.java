package ca.joeltherrien.randomforest.competingrisk;

import ca.joeltherrien.randomforest.TestUtils;
import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskFunctions;
import ca.joeltherrien.randomforest.utils.MathFunction;
import ca.joeltherrien.randomforest.utils.Point;
import ca.joeltherrien.randomforest.utils.Utils;
import org.junit.jupiter.api.Test;

import java.util.Collections;

public class TestCompetingRiskFunctions {

    @Test
    public void testCalculateEventSpecificMortality(){
        final MathFunction cif1 = new MathFunction(
                Utils.easyList(
                        new Point(1.0, 0.3),
                        new Point(1.5, 0.7),
                        new Point(2.0, 0.8)
                ), new Point(0.0 ,0.0)
        );

        // not being used
        final MathFunction chf1 = new MathFunction(Collections.emptyList());

        // not being used
        final MathFunction km = new MathFunction(Collections.emptyList());

        final CompetingRiskFunctions functions = CompetingRiskFunctions.builder()
                .causeSpecificHazards(Collections.singletonList(chf1))
                .cumulativeIncidenceCurves(Collections.singletonList(cif1))
                .survivalCurve(km)
                .build();

        final double mortality1_5 = functions.calculateEventSpecificMortality(1, 1.5);
        final double mortality3 = functions.calculateEventSpecificMortality(1, 3.0);
        final double mortality4 = functions.calculateEventSpecificMortality(1, 4.0);

        TestUtils.closeEnough(0.15, mortality1_5, 0.001);
        TestUtils.closeEnough(1.3, mortality3, 0.001);
        TestUtils.closeEnough(2.1, mortality4, 0.001);

    }


}
