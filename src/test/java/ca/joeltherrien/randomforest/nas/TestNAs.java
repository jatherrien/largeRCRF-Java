package ca.joeltherrien.randomforest.nas;

import ca.joeltherrien.randomforest.Row;
import ca.joeltherrien.randomforest.covariates.Covariate;
import ca.joeltherrien.randomforest.covariates.numeric.NumericCovariate;
import ca.joeltherrien.randomforest.responses.regression.MeanResponseCombiner;
import ca.joeltherrien.randomforest.responses.regression.WeightedVarianceGroupDifferentiator;
import ca.joeltherrien.randomforest.tree.TreeTrainer;
import ca.joeltherrien.randomforest.utils.Utils;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class TestNAs {

    private List<Row<Double>> generateData(List<Covariate> covariates){
        final List<Row<Double>> dataList = new ArrayList<>();


        // We must include an NA for one of the values
        dataList.add(Row.createSimple(Utils.easyMap("x", "NA"), covariates, 1, 5.0));
        dataList.add(Row.createSimple(Utils.easyMap("x", "1"), covariates, 1, 6.0));
        dataList.add(Row.createSimple(Utils.easyMap("x", "2"), covariates, 1, 5.5));
        dataList.add(Row.createSimple(Utils.easyMap("x", "7"), covariates, 1, 0.0));
        dataList.add(Row.createSimple(Utils.easyMap("x", "8"), covariates, 1, 1.0));
        dataList.add(Row.createSimple(Utils.easyMap("x", "8.4"), covariates, 1, 1.0));


        return dataList;
    }

    @Test
    public void testException(){
        // There was a bug with NAs where when we tried to randomly assign NAs during a split to the best split produced by NumericSplitRuleUpdater,
        // but NumericSplitRuleUpdater had unmodifiable lists when creating the split.
        // This bug verifies that this no longer causes a crash

        final List<Covariate> covariates = Collections.singletonList(new NumericCovariate("x", 0));
        final List<Row<Double>> dataset = generateData(covariates);

        final TreeTrainer<Double, Double> treeTrainer = TreeTrainer.<Double, Double>builder()
                .checkNodePurity(false)
                .covariates(covariates)
                .numberOfSplits(0)
                .nodeSize(1)
                .maxNodeDepth(1000)
                .groupDifferentiator(new WeightedVarianceGroupDifferentiator())
                .responseCombiner(new MeanResponseCombiner())
                .build();

        treeTrainer.growTree(dataset, new Random(123));

        // As long as no exception occurs, we passed


    }

}