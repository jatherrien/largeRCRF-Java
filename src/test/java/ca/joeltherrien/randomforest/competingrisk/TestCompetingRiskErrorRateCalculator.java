package ca.joeltherrien.randomforest.competingrisk;

import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskErrorRateCalculator;
import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestCompetingRiskErrorRateCalculator {

    @Test
    public void testConcordance(){

        final CompetingRiskResponse response1 = new CompetingRiskResponse(1, 5.0);
        final CompetingRiskResponse response2 = new CompetingRiskResponse(0, 6.0);
        final CompetingRiskResponse response3 = new CompetingRiskResponse(2, 8.0);
        final CompetingRiskResponse response4 = new CompetingRiskResponse(1, 3.0);

        final double[] mortalityArray = new double[]{1, 4, 3, 9};
        final List<CompetingRiskResponse> responseList = List.of(response1, response2, response3, response4);

        final int event = 1;

        final CompetingRiskErrorRateCalculator errorRateCalculator = new CompetingRiskErrorRateCalculator(new int[]{1,2}, null);

        final double concordance = errorRateCalculator.calculateConcordance(responseList, mortalityArray, event);

        // Expected value found through calculations by hand
        assertEquals(3.0/5.0, concordance);

    }

}
