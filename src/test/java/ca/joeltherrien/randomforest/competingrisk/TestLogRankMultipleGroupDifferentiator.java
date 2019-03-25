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

import ca.joeltherrien.randomforest.utils.DataUtils;
import ca.joeltherrien.randomforest.Row;
import ca.joeltherrien.randomforest.Settings;
import ca.joeltherrien.randomforest.covariates.Covariate;
import ca.joeltherrien.randomforest.covariates.settings.NumericCovariateSettings;
import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskResponse;
import ca.joeltherrien.randomforest.responses.competingrisk.differentiator.LogRankMultipleGroupDifferentiator;
import ca.joeltherrien.randomforest.tree.Split;
import ca.joeltherrien.randomforest.utils.SingletonIterator;
import ca.joeltherrien.randomforest.utils.Utils;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.AllArgsConstructor;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestLogRankMultipleGroupDifferentiator {

    private Iterator<Split<CompetingRiskResponse, ?>> turnIntoSplitIterator(List<Row<CompetingRiskResponse>> leftList,
                                                                                 List<Row<CompetingRiskResponse>> rightList){
        return new SingletonIterator<Split<CompetingRiskResponse, ?>>(new Split(null, leftList, rightList, Collections.emptyList()));
    }

    public static Data<CompetingRiskResponse> loadData(String filename) throws IOException {
        final ObjectNode yVarSettings = new ObjectNode(JsonNodeFactory.instance);
        yVarSettings.set("type", new TextNode("CompetingRiskResponse"));
        yVarSettings.set("delta", new TextNode("delta"));
        yVarSettings.set("u", new TextNode("u"));

        final Settings settings = Settings.builder()
                .trainingDataLocation(filename)
                .covariateSettings(
                        Utils.easyList(new NumericCovariateSettings("x2"))
                )
                .yVarSettings(yVarSettings)
                .build();

        final List<Covariate> covariates = settings.getCovariates();

        final DataUtils.ResponseLoader loader = settings.getResponseLoader();
        final List<Row<CompetingRiskResponse>> rows = DataUtils.loadData(covariates, loader, settings.getTrainingDataLocation());

        return new Data<>(rows, covariates);
    }

    @Test
    public void testSplitRule() throws IOException {
        final LogRankMultipleGroupDifferentiator groupDifferentiator = new LogRankMultipleGroupDifferentiator(new int[]{1,2});

        final List<Row<CompetingRiskResponse>> data = loadData("src/test/resources/test_split_data.csv").getRows();

        final List<Row<CompetingRiskResponse>> group1Bad = data.subList(0, 196);
        final List<Row<CompetingRiskResponse>> group2Bad = data.subList(196, data.size());

        final double scoreBad = groupDifferentiator.differentiate(turnIntoSplitIterator(group1Bad, group2Bad)).getScore();

        final List<Row<CompetingRiskResponse>> group1Good = data.subList(0, 199);
        final List<Row<CompetingRiskResponse>> group2Good= data.subList(199, data.size());

        final double scoreGood = groupDifferentiator.differentiate(turnIntoSplitIterator(group1Good, group2Good)).getScore();

        // expected results calculated manually using survival::survdiff in R; see issue #10 in Gitea
        closeEnough(71.41135, scoreBad, 0.00001);
        closeEnough(71.5354, scoreGood, 0.00001);

    }

    @Test
    public void testSplitRuleV2() throws IOException {
        final LogRankMultipleGroupDifferentiator groupDifferentiator = new LogRankMultipleGroupDifferentiator(new int[]{1,2});

        final List<Row<CompetingRiskResponse>> data = loadData("src/test/resources/new_data_that_triggers_difference_composite.csv").getRows();

        final List<Row<CompetingRiskResponse>> group1Bad = data.subList(0, 196);
        final List<Row<CompetingRiskResponse>> group2Bad = data.subList(196, data.size());

        final double scoreBad = groupDifferentiator.differentiate(turnIntoSplitIterator(group1Bad, group2Bad)).getScore();

        final List<Row<CompetingRiskResponse>> group1Good = data.subList(0, 199);
        final List<Row<CompetingRiskResponse>> group2Good= data.subList(199, data.size());

        final double scoreGood = groupDifferentiator.differentiate(turnIntoSplitIterator(group1Good, group2Good)).getScore();

        // expected results calculated manually using survival::survdiff in R; see issue #10 in Gitea
        closeEnough(71.41135, scoreBad, 0.00001);
        closeEnough(71.5354, scoreGood, 0.00001);

    }

    private void closeEnough(double expected, double actual, double margin){
        assertTrue(Math.abs(expected - actual) < margin, "Expected " + expected + " but saw " + actual);
    }

    @lombok.Data
    @AllArgsConstructor
    public static class Data<Y> {
        private List<Row<Y>> rows;
        private List<Covariate> covariateList;
    }

}
