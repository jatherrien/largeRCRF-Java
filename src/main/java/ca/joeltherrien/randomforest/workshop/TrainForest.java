package ca.joeltherrien.randomforest.workshop;

import ca.joeltherrien.randomforest.*;
import ca.joeltherrien.randomforest.regression.MeanResponseCombiner;
import ca.joeltherrien.randomforest.regression.WeightedVarianceGroupDifferentiator;
import ca.joeltherrien.randomforest.tree.Forest;
import ca.joeltherrien.randomforest.tree.ForestTrainer;
import ca.joeltherrien.randomforest.tree.TreeTrainer;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TrainForest {

    public static void main(String[] args){
        // test creating a regression tree on a problem and see if the results are sensible.

        final int n = 1000000;
        final int p = 5;

        final Random random = new Random();

        final List<Row<Double>> data = new ArrayList<>(n);

        double minY = 1000.0;

        for(int i=0; i<n; i++){
            double y = 0.0;
            final Map<String, Value> map = new HashMap<>();

            for(int j=0; j<p; j++){
                final double x = random.nextDouble();
                y+=x;

                map.put("x"+j, new NumericValue(x));
            }

            data.add(i, new Row<>(map, i, y));

            if(y < minY){
                minY = y;
            }

        }

        final List<String> covariateNames = IntStream.range(0, p).mapToObj(j -> "x"+j).collect(Collectors.toList());


        TreeTrainer<Double> treeTrainer = TreeTrainer.<Double>builder()
                .numberOfSplits(5)
                .nodeSize(3)
                .maxNodeDepth(100000000)
                .groupDifferentiator(new WeightedVarianceGroupDifferentiator())
                .responseCombiner(new MeanResponseCombiner())
                .build();

        final ForestTrainer<Double> forestTrainer = ForestTrainer.<Double>builder()
                .treeTrainer(treeTrainer)
                .data(data)
                .covariatesToTry(covariateNames)
                .mtry(4)
                .ntree(100)
                .treeResponseCombiner(new MeanResponseCombiner())
                .displayProgress(true)
                .build();

        final long startTime = System.currentTimeMillis();

        final Forest<Double> forest = forestTrainer.trainSerial();
        //final Forest<Double> forest = forestTrainer.trainParallel(8);

        final long endTime  = System.currentTimeMillis();

        System.out.println("Took " + (double)(endTime - startTime)/1000.0 + " seconds.");


        final Value zeroValue = new NumericValue(0.1);
        final Value point5Value = new NumericValue(0.5);

        // test row
        final CovariateRow testRow1 = new CovariateRow(Map.of("x0", zeroValue, "x1",zeroValue,"x2",zeroValue,"x3",zeroValue,"x4",zeroValue), 0);
        final CovariateRow testRow2 = new CovariateRow(Map.of("x0", point5Value, "x1",point5Value,"x2",point5Value,"x3",point5Value,"x4",point5Value), 2);


        System.out.println(forest.evaluate(testRow1));
        System.out.println(forest.evaluate(testRow2));

        System.out.println("MinY = " + minY);

    }

}
