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

package ca.joeltherrien.randomforest.nas;

import ca.joeltherrien.randomforest.Row;
import ca.joeltherrien.randomforest.covariates.Covariate;
import ca.joeltherrien.randomforest.covariates.bool.BooleanCovariate;
import ca.joeltherrien.randomforest.covariates.factor.FactorCovariate;
import ca.joeltherrien.randomforest.covariates.numeric.NumericCovariate;
import ca.joeltherrien.randomforest.responses.regression.MeanResponseCombiner;
import ca.joeltherrien.randomforest.responses.regression.WeightedVarianceSplitFinder;
import ca.joeltherrien.randomforest.tree.Split;
import ca.joeltherrien.randomforest.tree.TreeTrainer;
import ca.joeltherrien.randomforest.utils.Utils;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class TestNAs {

    private List<Row<Double>> generateData1(List<Covariate> covariates){
        final List<Row<Double>> dataList = new ArrayList<>();

        // We must include an NA for one of the values
        dataList.add(Row.createSimple(Utils.easyMap("x", "NA", "y", "true", "z", "green"), covariates, 1, 5.0));
        dataList.add(Row.createSimple(Utils.easyMap("x", "1", "y", "NA", "z", "blue"), covariates, 2, 6.0));
        dataList.add(Row.createSimple(Utils.easyMap("x", "2", "y", "true", "z", "NA"), covariates, 3, 5.5));
        dataList.add(Row.createSimple(Utils.easyMap("x", "7", "y", "false", "z", "green"), covariates, 4, 0.0));
        dataList.add(Row.createSimple(Utils.easyMap("x", "8", "y", "true", "z", "blue"), covariates, 5, 1.0));
        dataList.add(Row.createSimple(Utils.easyMap("x", "8.4", "y", "false", "z", "yellow"), covariates, 6, 1.0));


        return dataList;
    }

    @Test
    public void testException(){
        // There was a bug with NAs where when we tried to randomly assign NAs during a split to the best split produced by NumericSplitRuleUpdater,
        // but NumericSplitRuleUpdater had unmodifiable lists when creating the split.
        // This bug verifies that this no longer causes a crash

        final List<Covariate> covariates = Utils.easyList(
                new NumericCovariate("x", 0, false),
                new BooleanCovariate("y", 1, true),
                new FactorCovariate("z", 2, Utils.easyList("green", "blue", "yellow"), true)
                );
        final List<Row<Double>> dataset = generateData1(covariates);

        final TreeTrainer<Double, Double> treeTrainer = TreeTrainer.<Double, Double>builder()
                .checkNodePurity(false)
                .covariates(covariates)
                .numberOfSplits(0)
                .nodeSize(1)
                .mtry(3)
                .maxNodeDepth(1000)
                .splitFinder(new WeightedVarianceSplitFinder())
                .responseCombiner(new MeanResponseCombiner())
                .build();

        treeTrainer.growTree(dataset, new Random(123));

        // As long as no exception occurs, we passed
    }

    private List<Row<Double>> generateData2(List<Covariate> covariates){
        final List<Row<Double>> dataList = new ArrayList<>();
        // Idea - when ignoring NAs, BadVar gives a perfect split.
        // GoodVar is slightly worse than BadVar when NAs are excluded.
        // However, BadVar has a ton of NAs that will degrade its performance.
        dataList.add(Row.createSimple(
                Utils.easyMap("BadVar", "-1.0", "GoodVar", "true") // GoodVars one error
                , covariates, 1, 5.0)
        );
        dataList.add(Row.createSimple(
                Utils.easyMap("BadVar", "NA", "GoodVar", "false")
                , covariates, 2, 5.0)
        );
        dataList.add(Row.createSimple(
                Utils.easyMap("BadVar", "NA", "GoodVar", "false")
                , covariates, 3, 5.0)
        );
        dataList.add(Row.createSimple(
                Utils.easyMap("BadVar", "0.5", "GoodVar", "true")
                , covariates, 4, 10.0)
        );
        dataList.add(Row.createSimple(
                Utils.easyMap("BadVar", "NA", "GoodVar", "true")
                , covariates, 5, 10.0)
        );
        dataList.add(Row.createSimple(
                Utils.easyMap("BadVar", "NA", "GoodVar", "true")
                , covariates, 6, 10.0)
        );

        return dataList;
    }

    @Test
    // Test that the NA penalty works when selecting a best split.
    public void testNAPenalty(){
        final List<Covariate> covariates1 = Utils.easyList(
          new NumericCovariate("BadVar", 0, true),
          new BooleanCovariate("GoodVar", 1, false)
        );

        final List<Row<Double>> dataList1 = generateData2(covariates1);

        final TreeTrainer<Double, Double> treeTrainer1 = TreeTrainer.<Double, Double>builder()
                .checkNodePurity(false)
                .covariates(covariates1)
                .numberOfSplits(0)
                .nodeSize(1)
                .mtry(2)
                .maxNodeDepth(1000)
                .splitFinder(new WeightedVarianceSplitFinder())
                .responseCombiner(new MeanResponseCombiner())
                .build();

        final Split<Double, ?> bestSplit1 = treeTrainer1.findBestSplitRule(dataList1, covariates1, new Random(123));
        assertEquals(1, bestSplit1.getSplitRule().getParentCovariateIndex()); // 1 corresponds to GoodVar

        // Run again without the penalty; verify that we get different results

        final List<Covariate> covariates2 = Utils.easyList(
                new NumericCovariate("BadVar", 0, false),
                new BooleanCovariate("GoodVar", 1, false)
        );

        final List<Row<Double>> dataList2 = generateData2(covariates2);

        final TreeTrainer<Double, Double> treeTrainer2 = TreeTrainer.<Double, Double>builder()
                .checkNodePurity(false)
                .covariates(covariates2)
                .numberOfSplits(0)
                .nodeSize(1)
                .mtry(2)
                .maxNodeDepth(1000)
                .splitFinder(new WeightedVarianceSplitFinder())
                .responseCombiner(new MeanResponseCombiner())
                .build();

        final Split<Double, ?> bestSplit2 = treeTrainer2.findBestSplitRule(dataList2, covariates2, new Random(123));
        assertEquals(0, bestSplit2.getSplitRule().getParentCovariateIndex()); // 1 corresponds to GoodVar


    }

}
