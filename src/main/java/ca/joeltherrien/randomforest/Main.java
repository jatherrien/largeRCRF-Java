package ca.joeltherrien.randomforest;


import ca.joeltherrien.randomforest.regression.MeanGroupDifferentiator;
import ca.joeltherrien.randomforest.regression.MeanResponseCombiner;
import ca.joeltherrien.randomforest.regression.WeightedVarianceGroupDifferentiator;
import ca.joeltherrien.randomforest.tree.Node;
import ca.joeltherrien.randomforest.tree.TreeTrainer;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

public class Main {

	public static void main(String[] args) {
		System.out.println("Hello world!");

        final Random random = new Random(123);

        final int n = 1000;
        final List<Row<Double>> trainingSet = new ArrayList<>(n);

        final List<Value<Double>> x1List = DoubleStream
                .generate(() -> random.nextDouble()*10.0)
                .limit(n)
                .mapToObj(x1 -> new NumericValue(x1))
                .collect(Collectors.toList());

        final List<Value<Double>> x2List = DoubleStream
                .generate(() -> random.nextDouble()*10.0)
                .limit(n)
                .mapToObj(x1 -> new NumericValue(x1))
                .collect(Collectors.toList());



        for(int i=0; i<n; i++){
            double x1 = x1List.get(i).getValue();
            double x2 = x2List.get(i).getValue();

            trainingSet.add(generateRow(x1, x2, i));
        }


        final long startTime = System.currentTimeMillis();

        final TreeTrainer<Double> treeTrainer = TreeTrainer.<Double>builder()
                .groupDifferentiator(new WeightedVarianceGroupDifferentiator())
                .responseCombiner(new MeanResponseCombiner())
                .maxNodeDepth(30)
                .nodeSize(5)
                .numberOfSplits(0)
                .build();

        final long endTime = System.currentTimeMillis();

        System.out.println(((double)(endTime - startTime))/1000.0);

        final List<String> covariateNames = List.of("x1", "x2");

        final Node<Double> baseNode = treeTrainer.growTree(trainingSet, covariateNames);


        final List<CovariateRow> testSet = new ArrayList<>();
        testSet.add(generateCovariateRow(9, 2, 1)); // expect 1
        testSet.add(generateCovariateRow(5, 2, 5));
        testSet.add(generateCovariateRow(2, 2, 3));
        testSet.add(generateCovariateRow(9, 5, 0));
        testSet.add(generateCovariateRow(6, 5, 8));
        testSet.add(generateCovariateRow(3, 5, 10));
        testSet.add(generateCovariateRow(1, 5, 3));
        testSet.add(generateCovariateRow(7, 9, 2));
        testSet.add(generateCovariateRow(1, 9, 4));

        for(final CovariateRow testCase : testSet){
            System.out.println(testCase);
            System.out.println(baseNode.evaluate(testCase));
            System.out.println();


        }





    }

	public static Row<Double> generateRow(double x1, double x2, int id){
	    double y = generateResponse(x1, x2);

	    final Map<String, Value> map = Map.of("x1", new NumericValue(x1), "x2", new NumericValue(x2));

        return new Row<>(map, id, y);

    }


    public static CovariateRow generateCovariateRow(double x1, double x2, int id){
        final Map<String, Value> map = Map.of("x1", new NumericValue(x1), "x2", new NumericValue(x2));

        return new CovariateRow(map, id);

    }


	public static double generateResponse(double x1, double x2){
	    if(x2 <= 3){
	        if(x1 <= 3){
	            return 3;
            }
            else if(x1 <= 7){
	            return 5;
            }
            else{
	            return 1;
            }
        }
        else if(x1 >= 5){
	        if(x2 > 6){
	            return 2;
            }
            else if(x1 >= 8){
	            return 0;
            }
            else{
	            return 8;
            }
        }
        else if(x1 <= 2){
	        if(x2 >= 7){
	            return 4;
            }
            else{
	            return 3;
            }
        }
        else{
	        return 10;
        }


    }

}
