package ca.joeltherrien.randomforest.workshop;

import ca.joeltherrien.randomforest.*;
import ca.joeltherrien.randomforest.covariates.Covariate;
import ca.joeltherrien.randomforest.covariates.numeric.NumericCovariate;
import ca.joeltherrien.randomforest.responses.regression.MeanResponseCombiner;
import ca.joeltherrien.randomforest.responses.regression.WeightedVarianceGroupDifferentiator;
import ca.joeltherrien.randomforest.tree.ForestTrainer;
import ca.joeltherrien.randomforest.tree.TreeTrainer;

import java.util.*;

public class TrainForest {

    public static void main(String[] args){
        // test creating a regression tree on a problem and see if the results are sensible.

        final int n = 10000;
        final int p = 5;


        final Random random = new Random();

        final List<Row<Double>> data = new ArrayList<>(n);

        double minY = 1000.0;

        final List<Covariate> covariateList = new ArrayList<>(p);
        for(int j =0; j < p; j++){
            final NumericCovariate covariate = new NumericCovariate("x"+j, j);
            covariateList.add(covariate);
        }

        for(int i=0; i<n; i++){
            double y = 0.0;
            final Covariate.Value[] valueArray = new Covariate.Value[covariateList.size()];

            for(final Covariate covariate : covariateList) {
                final double x = random.nextDouble();
                y += x;

                valueArray[covariate.getIndex()] = covariate.createValue(y);
            }

            data.add(i, new Row<>(valueArray, i, y));

            if(y < minY){
                minY = y;
            }

        }


        final TreeTrainer<Double, Double> treeTrainer = TreeTrainer.<Double, Double>builder()
                .numberOfSplits(5)
                .nodeSize(5)
                .mtry(4)
                .maxNodeDepth(100000000)
                .groupDifferentiator(new WeightedVarianceGroupDifferentiator())
                .responseCombiner(new MeanResponseCombiner())
                .build();

        final ForestTrainer<Double, Double, Double> forestTrainer = ForestTrainer.<Double, Double, Double>builder()
                .treeTrainer(treeTrainer)
                .data(data)
                .covariates(covariateList)
                .ntree(100)
                .treeResponseCombiner(new MeanResponseCombiner())
                .displayProgress(true)
                .saveTreeLocation("/home/joel/test")
                .build();

        final long startTime = System.currentTimeMillis();

        //final Forest<Double> forest = forestTrainer.trainSerial();
        //final Forest<Double> forest = forestTrainer.trainParallelInMemory(3);
        forestTrainer.trainParallelOnDisk(3);

        final long endTime  = System.currentTimeMillis();

        System.out.println("Took " + (double)(endTime - startTime)/1000.0 + " seconds.");


        /*
        final Value zeroValue = new NumericValue(0.1);
        final Value point5Value = new NumericValue(0.5);

        // test row
        final CovariateRow testRow1 = new CovariateRow(Map.of("x0", zeroValue, "x1",zeroValue,"x2",zeroValue,"x3",zeroValue,"x4",zeroValue), 0);
        final CovariateRow testRow2 = new CovariateRow(Map.of("x0", point5Value, "x1",point5Value,"x2",point5Value,"x3",point5Value,"x4",point5Value), 2);


        System.out.println(forest.evaluate(testRow1));
        System.out.println(forest.evaluate(testRow2));
        */
        System.out.println("MinY = " + minY);

    }

}
