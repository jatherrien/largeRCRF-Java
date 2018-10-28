package ca.joeltherrien.randomforest.competingrisk;

import ca.joeltherrien.randomforest.Row;
import ca.joeltherrien.randomforest.covariates.Covariate;
import ca.joeltherrien.randomforest.responses.competingrisk.*;
import ca.joeltherrien.randomforest.tree.Forest;
import ca.joeltherrien.randomforest.utils.RightContinuousStepFunction;
import ca.joeltherrien.randomforest.utils.StepFunction;
import ca.joeltherrien.randomforest.utils.Utils;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static ca.joeltherrien.randomforest.TestUtils.closeEnough;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestCompetingRiskErrorRateCalculator {

    @Test
    public void testConcordance(){

        final CompetingRiskResponse response1 = new CompetingRiskResponse(1, 5.0);
        final CompetingRiskResponse response2 = new CompetingRiskResponse(0, 6.0);
        final CompetingRiskResponse response3 = new CompetingRiskResponse(2, 8.0);
        final CompetingRiskResponse response4 = new CompetingRiskResponse(1, 3.0);

        final double[] mortalityArray = new double[]{1, 4, 3, 9};
        final List<CompetingRiskResponse> responseList = Utils.easyList(response1, response2, response3, response4);

        final int event = 1;

        final Forest<CompetingRiskFunctions, CompetingRiskFunctions> fakeForest = Forest.<CompetingRiskFunctions, CompetingRiskFunctions>builder().build();

        final double naiveConcordance = CompetingRiskUtils.calculateConcordance(responseList, mortalityArray, event);

        final StepFunction fakeCensorDistribution = RightContinuousStepFunction.constructFromPoints(Collections.emptyList(), 1.0);
        // This distribution will make the IPCW weights == 1, giving identical results to the naive concordance.
        final double ipcwConcordance = CompetingRiskUtils.calculateIPCWConcordance(responseList, mortalityArray, event, fakeCensorDistribution);

        closeEnough(naiveConcordance, ipcwConcordance, 0.0001);

        // Expected value found through calculations by hand
        assertEquals(3.0/5.0, naiveConcordance);

    }


}
