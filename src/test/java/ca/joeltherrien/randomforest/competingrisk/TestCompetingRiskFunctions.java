package ca.joeltherrien.randomforest.competingrisk;

import ca.joeltherrien.randomforest.TestUtils;
import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskFunctions;
import ca.joeltherrien.randomforest.utils.Point;
import ca.joeltherrien.randomforest.utils.RightContinuousStepFunction;
import ca.joeltherrien.randomforest.utils.StepFunction;
import ca.joeltherrien.randomforest.utils.Utils;
import org.junit.jupiter.api.Test;

import java.util.Collections;

public class TestCompetingRiskFunctions {

    @Test
    public void testCalculateEventSpecificMortality(){
        final StepFunction cif1 = RightContinuousStepFunction.constructFromPoints(
                Utils.easyList(
                        new Point(1.0, 0.3),
                        new Point(1.5, 0.7),
                        new Point(2.0, 0.8)
                ), 0.0
        );

        // not being used
        final StepFunction chf1 = RightContinuousStepFunction.constructFromPoints(Collections.emptyList(), 0.0);

        // not being used
        final StepFunction km = RightContinuousStepFunction.constructFromPoints(Collections.emptyList(), 0.0);

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
