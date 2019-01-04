package ca.joeltherrien.randomforest.competingrisk;

import ca.joeltherrien.randomforest.Row;
import ca.joeltherrien.randomforest.responses.competingrisk.*;
import ca.joeltherrien.randomforest.responses.competingrisk.differentiator.LogRankMultipleGroupDifferentiator;
import ca.joeltherrien.randomforest.responses.competingrisk.differentiator.LogRankSingleGroupDifferentiator;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestLogRankSingleGroupDifferentiator {

    private List<CompetingRiskResponse> generateData1(){
        final List<CompetingRiskResponse> data = new ArrayList<>();

        data.add(new CompetingRiskResponse(1, 1.0));
        data.add(new CompetingRiskResponse(1, 1.0));
        data.add(new CompetingRiskResponse(1, 2.0));
        data.add(new CompetingRiskResponse(1, 1.5));
        data.add(new CompetingRiskResponse(0, 2.0));
        data.add(new CompetingRiskResponse(0, 1.5));
        data.add(new CompetingRiskResponse(0, 2.5));

        return data;
    }

    private List<CompetingRiskResponse> generateData2(){
        final List<CompetingRiskResponse> data = new ArrayList<>();

        data.add(new CompetingRiskResponse(1, 2.0));
        data.add(new CompetingRiskResponse(1, 2.0));
        data.add(new CompetingRiskResponse(1, 4.0));
        data.add(new CompetingRiskResponse(1, 3.0));
        data.add(new CompetingRiskResponse(0, 4.0));
        data.add(new CompetingRiskResponse(0, 3.0));
        data.add(new CompetingRiskResponse(0, 5.0));

        return data;
    }

    @Test
    public void testCompetingRiskResponseCombiner(){
        final List<CompetingRiskResponse> data1 = generateData1();
        final List<CompetingRiskResponse> data2 = generateData2();

        final LogRankSingleGroupDifferentiator differentiator = new LogRankSingleGroupDifferentiator(1, new int[]{1});

        final double score = differentiator.differentiate(data1, data2);
        final double margin = 0.000001;

        // Tested using 855 method
        closeEnough(1.540139, score, margin);


    }

    @Test
    public void testCorrectSplit() throws IOException {
        final LogRankSingleGroupDifferentiator groupDifferentiator =
                new LogRankSingleGroupDifferentiator(1, new int[]{1,2});

        final List<Row<CompetingRiskResponse>> data = TestLogRankMultipleGroupDifferentiator.
                loadData("src/test/resources/test_single_split.csv").getRows();

        final List<Row<CompetingRiskResponse>> group1Good = data.subList(0, 221);
        final List<Row<CompetingRiskResponse>> group2Good = data.subList(221, data.size());

        final double scoreGood = groupDifferentiator.differentiate(
                group1Good.stream().map(Row::getResponse).collect(Collectors.toList()),
                group2Good.stream().map(Row::getResponse).collect(Collectors.toList()));

        final List<Row<CompetingRiskResponse>> group1Bad = data.subList(0, 222);
        final List<Row<CompetingRiskResponse>> group2Bad = data.subList(222, data.size());

        final double scoreBad = groupDifferentiator.differentiate(
                group1Bad.stream().map(Row::getResponse).collect(Collectors.toList()),
                group2Bad.stream().map(Row::getResponse).collect(Collectors.toList()));

        // Apparently not all groups are unique when splitting
        assertEquals(scoreGood, scoreBad);
    }

    private void closeEnough(double expected, double actual, double margin){
        assertTrue(Math.abs(expected - actual) < margin, "Expected " + expected + " but saw " + actual);
    }

}
