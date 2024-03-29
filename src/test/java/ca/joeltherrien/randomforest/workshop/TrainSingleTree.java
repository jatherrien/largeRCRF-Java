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


import ca.joeltherrien.randomforest.covariates.Covariate;
import ca.joeltherrien.randomforest.CovariateRow;
import ca.joeltherrien.randomforest.covariates.numeric.NumericCovariate;
import ca.joeltherrien.randomforest.Row;
import ca.joeltherrien.randomforest.responses.regression.MeanResponseCombiner;
import ca.joeltherrien.randomforest.responses.regression.WeightedVarianceSplitFinder;
import ca.joeltherrien.randomforest.tree.Node;
import ca.joeltherrien.randomforest.tree.TreeTrainer;
import ca.joeltherrien.randomforest.utils.Utils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

public class TrainSingleTree {

	public static void main(String[] args) {
        final Random random = new Random(123);

        final int n = 1000;
        final List<Row<Double>> trainingSet = new ArrayList<>(n);

        final Covariate<Double> x1Covariate = new NumericCovariate("x1", 0, false);
        final Covariate<Double> x2Covariate = new NumericCovariate("x2", 1, false);

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



        for(int i=0; i<n; i++){
            final Covariate.Value<Double> x1 = x1List.get(i);
            final Covariate.Value<Double> x2 = x2List.get(i);

            trainingSet.add(generateRow(x1, x2, i));
        }

        final List<Covariate> covariateNames = Utils.easyList(x1Covariate, x2Covariate);

        final TreeTrainer<Double, Double> treeTrainer = TreeTrainer.<Double, Double>builder()
                .splitFinder(new WeightedVarianceSplitFinder())
                .covariates(covariateNames)
                .responseCombiner(new MeanResponseCombiner())
                .maxNodeDepth(30)
                .mtry(2)
                .nodeSize(5)
                .numberOfSplits(0)
                .build();


        final long startTime = System.currentTimeMillis();
        final Node<Double> baseNode = treeTrainer.growTree(trainingSet, new Random());
        final long endTime = System.currentTimeMillis();

        System.out.println(((double)(endTime - startTime))/1000.0);






        final List<CovariateRow> testSet = new ArrayList<>();
        testSet.add(generateCovariateRow(x1Covariate.createValue(9.0), x2Covariate.createValue(2.0), 1)); // expect 1
        testSet.add(generateCovariateRow(x1Covariate.createValue(5.0), x2Covariate.createValue(2.0), 5));
        testSet.add(generateCovariateRow(x1Covariate.createValue(2.0), x2Covariate.createValue(2.0), 3));
        testSet.add(generateCovariateRow(x1Covariate.createValue(9.0), x2Covariate.createValue(5.0), 0));
        testSet.add(generateCovariateRow(x1Covariate.createValue(6.0), x2Covariate.createValue(5.0), 8));
        testSet.add(generateCovariateRow(x1Covariate.createValue(3.0), x2Covariate.createValue(5.0), 10));
        testSet.add(generateCovariateRow(x1Covariate.createValue(1.0), x2Covariate.createValue(5.0), 3));
        testSet.add(generateCovariateRow(x1Covariate.createValue(7.0), x2Covariate.createValue(9.0), 2));
        testSet.add(generateCovariateRow(x1Covariate.createValue(1.0), x2Covariate.createValue(9.0), 4));

        for(final CovariateRow testCase : testSet){
            System.out.println(testCase);
            System.out.println(baseNode.evaluate(testCase));
            System.out.println();


        }





    }

	public static Row<Double> generateRow(Covariate.Value<Double> x1, Covariate.Value<Double> x2, int id){
	    double y = generateResponse(x1.getValue(), x2.getValue());

	    final Covariate.Value[] valueArray = new Covariate.Value[2];
	    valueArray[0] = x1;
	    valueArray[1] = x2;

        return new Row<>(valueArray, id, y);

    }


    public static CovariateRow generateCovariateRow(Covariate.Value x1, Covariate.Value x2, int id){
        final Covariate.Value[] valueArray = new Covariate.Value[2];
        valueArray[0] = x1;
        valueArray[1] = x2;

        return new CovariateRow(valueArray, id);

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
