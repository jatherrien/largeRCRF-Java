package ca.joeltherrien.randomforest.competingrisk;

import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskErrorRateCalculator;
import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestCompetingRiskErrorRateCalculator {

    /*
    @Test
    public void testComparingResponses(){

        // Large, uncensored
        CompetingRiskResponse responseA = new CompetingRiskResponse(1, 10.0);

        // Large, censored
        CompetingRiskResponse responseB = new CompetingRiskResponse(0, 10.0);

        // Large, other event
        CompetingRiskResponse responseC = new CompetingRiskResponse(2, 10.0);

        // Medium, uncensored
        CompetingRiskResponse responseD = new CompetingRiskResponse(1, 5.0);

        // Medium, censored
        CompetingRiskResponse responseE = new CompetingRiskResponse(0, 5.0);

        // Medium, other event
        CompetingRiskResponse responseF = new CompetingRiskResponse(2, 5.0);

        final int event = 1;

        final CompetingRiskErrorRateCalculator errorRateCalculator = new CompetingRiskErrorRateCalculator(null, null);

        assertThrows(IllegalArgumentException.class, () -> errorRateCalculator.compare(responseB, responseB, event));
        assertThrows(IllegalArgumentException.class, () -> errorRateCalculator.compare(responseC, responseC, event));

        assertEquals(0.5, errorRateCalculator.compare(responseA, responseB, event));
        assertEquals(-0.5, errorRateCalculator.compare(responseB, responseA, event));

        assertEquals(0.0, errorRateCalculator.compare(responseA, responseA, event));

        assertThrows(IllegalArgumentException.class, () -> errorRateCalculator.compare(responseB, responseE, event));
        assertThrows(IllegalArgumentException.class, () -> errorRateCalculator.compare(responseE, responseB, event));
        assertThrows(IllegalArgumentException.class, () -> errorRateCalculator.compare(responseB, responseF, event));
        assertThrows(IllegalArgumentException.class, () -> errorRateCalculator.compare(responseF, responseB, event));

        assertEquals(-1.0, errorRateCalculator.compare(responseB, responseD, event));
        assertEquals(1.0, errorRateCalculator.compare(responseD, responseB, event));
        assertEquals(-1.0, errorRateCalculator.compare(responseC, responseD, event));
        assertEquals(1.0, errorRateCalculator.compare(responseD, responseC, event));

        assertEquals(-1.0, errorRateCalculator.compare(responseA, responseD, event));
        assertEquals(1.0, errorRateCalculator.compare(responseD, responseA, event));

        assertThrows(IllegalArgumentException.class, () -> errorRateCalculator.compare(responseA, responseE, event));
        assertThrows(IllegalArgumentException.class, () -> errorRateCalculator.compare(responseE, responseA, event));
        assertThrows(IllegalArgumentException.class, () -> errorRateCalculator.compare(responseA, responseF, event));
        assertThrows(IllegalArgumentException.class, () -> errorRateCalculator.compare(responseF, responseA, event));


    }
    */

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

        final double concordance = errorRateCalculator.calculate(responseList, mortalityArray, event);

        // Expected value found through calculations by hand
        assertEquals(3.0/5.0, concordance);

    }

}
