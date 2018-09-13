package ca.joeltherrien.randomforest.competingrisk;

import ca.joeltherrien.randomforest.Row;
import ca.joeltherrien.randomforest.responses.competingrisk.*;
import ca.joeltherrien.randomforest.tree.Forest;
import ca.joeltherrien.randomforest.utils.MathFunction;
import ca.joeltherrien.randomforest.utils.Point;
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

        final MathFunction fakeCensorDistribution = new MathFunction(Collections.emptyList(), new Point(0.0, 1.0));
        // This distribution will make the IPCW weights == 1, giving identical results to the naive concordance.
        final double ipcwConcordance = CompetingRiskUtils.calculateIPCWConcordance(responseList, mortalityArray, event, fakeCensorDistribution);

        closeEnough(naiveConcordance, ipcwConcordance, 0.0001);

        // Expected value found through calculations by hand
        assertEquals(3.0/5.0, naiveConcordance);

    }

    @Test
    public void testNaiveMortality(){
        final CompetingRiskResponse response1 = new CompetingRiskResponse(1, 5.0);
        final CompetingRiskResponse response2 = new CompetingRiskResponse(0, 6.0);
        final CompetingRiskResponse response3 = new CompetingRiskResponse(2, 8.0);
        final CompetingRiskResponse response4 = new CompetingRiskResponse(1, 3.0);

        final List<Row<CompetingRiskResponse>> dataset = Utils.easyList(
                new Row<>(Collections.emptyMap(), 1, response1),
                new Row<>(Collections.emptyMap(), 2, response2),
                new Row<>(Collections.emptyMap(), 3, response3),
                new Row<>(Collections.emptyMap(), 4, response4)
        );

        final double[] mortalityOneArray = new double[]{1, 4, 3, 9};
        final double[] mortalityTwoArray = new double[]{2, 3, 4, 7};

        // response1 was predicted incorrectly
        // response2 doesn't matter; censored
        // response3 was correctly predicted
        // response4 was correctly predicted

        // Expect 1/3 for my error

        final CompetingRiskFunctions function1 = mock(CompetingRiskFunctions.class);
        when(function1.calculateEventSpecificMortality(1, response1.getU())).thenReturn(mortalityOneArray[0]);
        when(function1.calculateEventSpecificMortality(2, response1.getU())).thenReturn(mortalityTwoArray[0]);

        final CompetingRiskFunctions function2 = mock(CompetingRiskFunctions.class);
        when(function2.calculateEventSpecificMortality(1, response2.getU())).thenReturn(mortalityOneArray[1]);
        when(function2.calculateEventSpecificMortality(2, response2.getU())).thenReturn(mortalityTwoArray[1]);

        final CompetingRiskFunctions function3 = mock(CompetingRiskFunctions.class);
        when(function3.calculateEventSpecificMortality(1, response3.getU())).thenReturn(mortalityOneArray[2]);
        when(function3.calculateEventSpecificMortality(2, response3.getU())).thenReturn(mortalityTwoArray[2]);

        final CompetingRiskFunctions function4 = mock(CompetingRiskFunctions.class);
        when(function4.calculateEventSpecificMortality(1, response4.getU())).thenReturn(mortalityOneArray[3]);
        when(function4.calculateEventSpecificMortality(2, response4.getU())).thenReturn(mortalityTwoArray[3]);

        final Forest mockForest = mock(Forest.class);
        when(mockForest.evaluateOOB(dataset.get(0))).thenReturn(function1);
        when(mockForest.evaluateOOB(dataset.get(1))).thenReturn(function2);
        when(mockForest.evaluateOOB(dataset.get(2))).thenReturn(function3);
        when(mockForest.evaluateOOB(dataset.get(3))).thenReturn(function4);


        final CompetingRiskErrorRateCalculator errorRateCalculator = new CompetingRiskErrorRateCalculator(dataset, mockForest, true);

        final double error = errorRateCalculator.calculateNaiveMortalityError(new int[]{1,2});

        assertEquals(1.0/3.0, error);

    }



}
