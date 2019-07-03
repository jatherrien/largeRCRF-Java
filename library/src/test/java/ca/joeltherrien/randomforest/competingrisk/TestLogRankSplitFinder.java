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
import ca.joeltherrien.randomforest.TestUtils;
import ca.joeltherrien.randomforest.covariates.Covariate;
import ca.joeltherrien.randomforest.covariates.numeric.NumericCovariate;
import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskResponse;
import ca.joeltherrien.randomforest.responses.competingrisk.splitfinder.LogRankSplitFinder;
import ca.joeltherrien.randomforest.tree.Split;
import ca.joeltherrien.randomforest.utils.Data;
import ca.joeltherrien.randomforest.utils.ResponseLoader;
import ca.joeltherrien.randomforest.utils.SingletonIterator;
import ca.joeltherrien.randomforest.utils.Utils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestLogRankSplitFinder {

    private Iterator<Split<CompetingRiskResponse, ?>> turnIntoSplitIterator(List<Row<CompetingRiskResponse>> leftList,
                                                                                 List<Row<CompetingRiskResponse>> rightList){
        return new SingletonIterator<Split<CompetingRiskResponse, ?>>(new Split(null, leftList, rightList, Collections.emptyList()));
    }

    public static Data<CompetingRiskResponse> loadData(String filename) throws IOException {

        final List<Covariate> covariates = Utils.easyList(
                new NumericCovariate("x2", 0)
        );

        final List<Row<CompetingRiskResponse>> rows = TestUtils.loadData(covariates, new ResponseLoader.CompetingRisksResponseLoader("delta", "u"), filename);

        return new Data<>(rows, covariates);
    }

    @Test
    public void testSplitRule() throws IOException {
        final LogRankSplitFinder splitFinder = new LogRankSplitFinder(new int[]{1,2}, new int[]{1,2});

        final List<Row<CompetingRiskResponse>> data = loadData("src/test/resources/test_split_data.csv").getRows();

        final List<Row<CompetingRiskResponse>> group1Bad = data.subList(0, 196);
        final List<Row<CompetingRiskResponse>> group2Bad = data.subList(196, data.size());

        final double scoreBad = splitFinder.findBestSplit(turnIntoSplitIterator(group1Bad, group2Bad)).getScore();

        // expected results calculated manually using survival::survdiff in R; see issue #10 in Gitea
        closeEnough(9.413002, scoreBad, 0.00001);

    }


    private void closeEnough(double expected, double actual, double margin){
        assertTrue(Math.abs(expected - actual) < margin, "Expected " + expected + " but saw " + actual);
    }

}
