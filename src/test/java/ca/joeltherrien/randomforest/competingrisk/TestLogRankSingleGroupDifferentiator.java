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

package ca.joeltherrien.randomforest.competingrisk;

import ca.joeltherrien.randomforest.Row;
import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskResponse;
import ca.joeltherrien.randomforest.responses.competingrisk.differentiator.LogRankSingleGroupDifferentiator;
import ca.joeltherrien.randomforest.tree.GroupDifferentiator;
import ca.joeltherrien.randomforest.tree.Split;
import ca.joeltherrien.randomforest.utils.SingletonIterator;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestLogRankSingleGroupDifferentiator {

    private double getScore(final GroupDifferentiator<CompetingRiskResponse> groupDifferentiator, List<Row<CompetingRiskResponse>> left, List<Row<CompetingRiskResponse>> right){
        final Iterator<Split<CompetingRiskResponse, ?>> iterator = new SingletonIterator<>(
                new Split<>(null, left, right, Collections.emptyList()));

        return groupDifferentiator.differentiate(iterator).getScore();

    }

    int count = 1;
    private <Y> Row<Y> createRow(Y response){
        return new Row<>(null, count++, response);
    }

    private List<Row<CompetingRiskResponse>> generateData1(){
        final List<Row<CompetingRiskResponse>> data = new ArrayList<>();

        data.add(createRow(new CompetingRiskResponse(1, 1.0)));
        data.add(createRow(new CompetingRiskResponse(1, 1.0)));
        data.add(createRow(new CompetingRiskResponse(1, 2.0)));
        data.add(createRow(new CompetingRiskResponse(1, 1.5)));
        data.add(createRow(new CompetingRiskResponse(0, 2.0)));
        data.add(createRow(new CompetingRiskResponse(0, 1.5)));
        data.add(createRow(new CompetingRiskResponse(0, 2.5)));

        return data;
    }

    private List<Row<CompetingRiskResponse>> generateData2(){
        final List<Row<CompetingRiskResponse>> data = new ArrayList<>();

        data.add(createRow(new CompetingRiskResponse(1, 2.0)));
        data.add(createRow(new CompetingRiskResponse(1, 2.0)));
        data.add(createRow(new CompetingRiskResponse(1, 4.0)));
        data.add(createRow(new CompetingRiskResponse(1, 3.0)));
        data.add(createRow(new CompetingRiskResponse(0, 4.0)));
        data.add(createRow(new CompetingRiskResponse(0, 3.0)));
        data.add(createRow(new CompetingRiskResponse(0, 5.0)));

        return data;
    }

    @Test
    public void testCompetingRiskResponseCombiner(){
        final List<Row<CompetingRiskResponse>> data1 = generateData1();
        final List<Row<CompetingRiskResponse>> data2 = generateData2();

        final LogRankSingleGroupDifferentiator differentiator = new LogRankSingleGroupDifferentiator(1, new int[]{1});

        final double score = getScore(differentiator, data1, data2);
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

        final double scoreGood = getScore(groupDifferentiator, group1Good, group2Good);

        final List<Row<CompetingRiskResponse>> group1Bad = data.subList(0, 222);
        final List<Row<CompetingRiskResponse>> group2Bad = data.subList(222, data.size());

        final double scoreBad = getScore(groupDifferentiator, group1Bad, group2Bad);

        // Apparently not all groups are unique when splitting
        assertEquals(scoreGood, scoreBad);
    }

    private void closeEnough(double expected, double actual, double margin){
        assertTrue(Math.abs(expected - actual) < margin, "Expected " + expected + " but saw " + actual);
    }

}
