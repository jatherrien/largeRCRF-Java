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

package ca.joeltherrien.randomforest.workshop;


import ca.joeltherrien.randomforest.*;
import ca.joeltherrien.randomforest.covariates.Covariate;
import ca.joeltherrien.randomforest.covariates.factor.FactorCovariate;
import ca.joeltherrien.randomforest.covariates.numeric.NumericCovariate;
import ca.joeltherrien.randomforest.responses.regression.MeanResponseCombiner;
import ca.joeltherrien.randomforest.responses.regression.WeightedVarianceGroupDifferentiator;
import ca.joeltherrien.randomforest.tree.Node;
import ca.joeltherrien.randomforest.tree.TreeTrainer;
import ca.joeltherrien.randomforest.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

public class TrainSingleTreeFactor {

	public static void main(String[] args) {
        final Random random = new Random(123);

        final int n = 10000;
        final List<Row<Double>> trainingSet = new ArrayList<>(n);

        final Covariate<Double> x1Covariate = new NumericCovariate("x1", 0);
        final Covariate<Double> x2Covariate = new NumericCovariate("x2", 1);
        final FactorCovariate x3Covariate = new FactorCovariate("x3", 2, Utils.easyList("cat", "dog", "mouse"));

        final List<Covariate.Value<Double>> x1List = DoubleStream
                .generate(() -> random.nextDouble()*10.0)
                .limit(n)
                .mapToObj(x1 -> x1Covariate.createValue(x1))
                .collect(Collectors.toList());

        final List<Covariate.Value<Double>> x2List = DoubleStream
                .generate(() -> random.nextDouble()*10.0)
                .limit(n)
                .mapToObj(x2 -> x1Covariate.createValue(x2))
                .collect(Collectors.toList());

        final List<Covariate.Value<String>> x3List = DoubleStream
                .generate(() -> random.nextDouble())
                .limit(n)
                .mapToObj(db -> {
                    if(db < 0.333){
                        return "cat";
                    }
                    else if(db < 0.5){
                        return "mouse";
                    }
                    else{
                        return "dog";
                    }
                })
                .map(str -> x3Covariate.createValue(str))
                .collect(Collectors.toList());


        for(int i=0; i<n; i++){
            final Covariate.Value<Double> x1 = x1List.get(i);
            final Covariate.Value<Double> x2 = x2List.get(i);
            final Covariate.Value<String> x3 = x3List.get(i);

            trainingSet.add(generateRow(x1, x2, x3, i));
        }

        final List<Covariate> covariateNames = Utils.easyList(x1Covariate, x2Covariate);

        final TreeTrainer<Double, Double> treeTrainer = TreeTrainer.<Double, Double>builder()
                .groupDifferentiator(new WeightedVarianceGroupDifferentiator())
                .responseCombiner(new MeanResponseCombiner())
                .covariates(covariateNames)
                .maxNodeDepth(30)
                .nodeSize(5)
                .numberOfSplits(5)
                .build();



        final long startTime = System.currentTimeMillis();
        final Node<Double> baseNode = treeTrainer.growTree(trainingSet, random);
        final long endTime = System.currentTimeMillis();

        System.out.println(((double)(endTime - startTime))/1000.0);



        final Covariate.Value<String> cat = x3Covariate.createValue("cat");
        final Covariate.Value<String> dog = x3Covariate.createValue("dog");
        final Covariate.Value<String> mouse = x3Covariate.createValue("mouse");


        final List<CovariateRow> testSet = new ArrayList<>();
        testSet.add(generateCovariateRow(x1Covariate.createValue(9.0), x2Covariate.createValue(2.0), cat, 1)); // expect 1
        testSet.add(generateCovariateRow(x1Covariate.createValue(5.0), x2Covariate.createValue(2.0), dog, 5));
        testSet.add(generateCovariateRow(x1Covariate.createValue(2.0), x2Covariate.createValue(2.0), cat, 3));
        testSet.add(generateCovariateRow(x1Covariate.createValue(9.0), x2Covariate.createValue(5.0), dog, 0));
        testSet.add(generateCovariateRow(x1Covariate.createValue(6.0), x2Covariate.createValue(5.0), cat, 8));
        testSet.add(generateCovariateRow(x1Covariate.createValue(3.0), x2Covariate.createValue(5.0), dog, 10));
        testSet.add(generateCovariateRow(x1Covariate.createValue(1.0), x2Covariate.createValue(5.0), cat, 3));
        testSet.add(generateCovariateRow(x1Covariate.createValue(7.0), x2Covariate.createValue(9.0), dog, 2));
        testSet.add(generateCovariateRow(x1Covariate.createValue(1.0), x2Covariate.createValue(9.0), cat, 4));


        testSet.add(generateCovariateRow(x1Covariate.createValue(3.0), x2Covariate.createValue(9.0), mouse, 0));
        testSet.add(generateCovariateRow(x1Covariate.createValue(7.0), x2Covariate.createValue(9.0), mouse, 5));

        for(final CovariateRow testCase : testSet){
            System.out.println(testCase);
            System.out.println(baseNode.evaluate(testCase));
            System.out.println();


        }





    }

	public static Row<Double> generateRow(Covariate.Value<Double> x1, Covariate.Value<Double> x2, Covariate.Value<String> x3, int id){
	    double y = generateResponse(x1.getValue(), x2.getValue(), x3.getValue());

        final Covariate.Value[] valueArray = new Covariate.Value[3];
        valueArray[0] = x1;
        valueArray[1] = x2;
        valueArray[2] = x3;

	    //final Map<String, Covariate.Value> map = Utils.easyMap("x1", x1, "x2", x2); // Missing x3?

        return new Row<>(valueArray, id, y);

    }


    public static CovariateRow generateCovariateRow(Covariate.Value x1, Covariate.Value x2, Covariate.Value x3, int id){
        final Covariate.Value[] valueArray = new Covariate.Value[3];
        valueArray[0] = x1;
        valueArray[1] = x2;
        valueArray[2] = x3;

        return new CovariateRow(valueArray, id);

    }


	public static double generateResponse(double x1, double x2, String x3){

	    if(x3.equalsIgnoreCase("mouse")){
	        if(x1 <= 5){
	            return 0;
            }
            else{
	            return 5;
            }
        }

        // cat & dog below

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
