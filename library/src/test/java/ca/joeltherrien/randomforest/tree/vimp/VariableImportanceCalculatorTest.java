package ca.joeltherrien.randomforest.tree.vimp;

import ca.joeltherrien.randomforest.Row;
import ca.joeltherrien.randomforest.covariates.Covariate;
import ca.joeltherrien.randomforest.covariates.bool.BooleanCovariate;
import ca.joeltherrien.randomforest.covariates.bool.BooleanSplitRule;
import ca.joeltherrien.randomforest.covariates.factor.FactorCovariate;
import ca.joeltherrien.randomforest.covariates.numeric.NumericCovariate;
import ca.joeltherrien.randomforest.covariates.numeric.NumericSplitRule;
import ca.joeltherrien.randomforest.responses.regression.MeanResponseCombiner;
import ca.joeltherrien.randomforest.responses.regression.WeightedVarianceSplitFinder;
import ca.joeltherrien.randomforest.tree.*;
import ca.joeltherrien.randomforest.utils.Utils;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class VariableImportanceCalculatorTest {

    /*
        Since the logic for VariableImportanceCalculator is generic, it will be much easier to test under a regression
        setting.
     */

    // We'l have a very simple Forest of two trees
    private final Forest<Double, Double> forest;


    private final List<Covariate> covariates;
    private final List<Row<Double>> rowList;

    /*
        Long setup process; forest is manually constructed so that we can be exactly sure on our variable importance.

     */
    public VariableImportanceCalculatorTest(){
        final BooleanCovariate booleanCovariate = new BooleanCovariate("x", 0);
        final NumericCovariate numericCovariate = new NumericCovariate("y", 1);
        final FactorCovariate factorCovariate = new FactorCovariate("z", 2,
                Utils.easyList("red", "blue", "green"));

        this.covariates = Utils.easyList(booleanCovariate, numericCovariate, factorCovariate);

        final TreeTrainer<Double, Double> treeTrainer = TreeTrainer.<Double, Double>builder()
            .responseCombiner(new MeanResponseCombiner())
            .splitFinder(new WeightedVarianceSplitFinder())
             .numberOfSplits(0)
             .nodeSize(1)
             .maxNodeDepth(100)
             .mtry(3)
             .checkNodePurity(false)
             .covariates(this.covariates)
             .build();

        /*
            Plan for data - BooleanCovariate is split on first and has the largest impact.
            NumericCovariate is at second level and has more minimal impact.
            FactorCovariate is useless and never used.
            Our tree (we'll duplicate it for testing OOB errors) will have a depth of 1. (0 based).
         */

        final Tree<Double> tree1 = makeTree(covariates, 0.0, new int[]{1,2,3,4});
        final Tree<Double> tree2 = makeTree(covariates, 2.0, new int[]{5,6,7,8});

        this.forest = Forest.<Double, Double>builder()
                .trees(Utils.easyList(tree1, tree2))
                .treeResponseCombiner(new MeanResponseCombiner())
                .covariateList(this.covariates)
                .build();

        // formula; boolean high adds 100; high numeric adds 10
        // This row list should have a baseline error of 0.0
        this.rowList = Utils.easyList(
                Row.createSimple(Utils.easyMap(
                        "x", "false",
                        "y", "0.0",
                        "z", "red"),
                        covariates, 1, 0.0
                ),
                Row.createSimple(Utils.easyMap(
                        "x", "false",
                        "y", "10.0",
                        "z", "blue"),
                        covariates, 2, 10.0
                ),
                Row.createSimple(Utils.easyMap(
                        "x", "true",
                        "y", "0.0",
                        "z", "red"),
                        covariates, 3, 100.0
                ),
                Row.createSimple(Utils.easyMap(
                        "x", "true",
                        "y", "10.0",
                        "z", "green"),
                        covariates, 4, 110.0
                ),

                Row.createSimple(Utils.easyMap(
                        "x", "false",
                        "y", "0.0",
                        "z", "red"),
                        covariates, 5, 0.0
                ),
                Row.createSimple(Utils.easyMap(
                        "x", "false",
                        "y", "10.0",
                        "z", "blue"),
                        covariates, 6, 10.0
                ),
                Row.createSimple(Utils.easyMap(
                        "x", "true",
                        "y", "0.0",
                        "z", "red"),
                        covariates, 7, 100.0
                ),
                Row.createSimple(Utils.easyMap(
                        "x", "true",
                        "y", "10.0",
                        "z", "green"),
                        covariates, 8, 110.0
                )
        );
    }

    private Tree<Double> makeTree(List<Covariate> covariates, double offset, int[] indices){
        // Naming convention - xyTerminal where x and y are low/high denotes whether BooleanCovariate(x) is low/high and
        // whether NumericCovariate(y) is low/high.
        final TerminalNode<Double> lowLowTerminal = new TerminalNode<>(0.0 + offset, 5);
        final TerminalNode<Double> lowHighTerminal = new TerminalNode<>(10.0 + offset, 5);
        final TerminalNode<Double> highLowTerminal = new TerminalNode<>(100.0 + offset, 5);
        final TerminalNode<Double> highHighTerminal = new TerminalNode<>(110.0 + offset, 5);

        final SplitNode<Double> lowSplitNode = SplitNode.<Double>builder()
                .leftHand(lowLowTerminal)
                .rightHand(lowHighTerminal)
                .probabilityNaLeftHand(0.5)
                .splitRule(new NumericSplitRule((NumericCovariate) covariates.get(1), 5.0))
                .build();

        final SplitNode<Double> highSplitNode = SplitNode.<Double>builder()
                .leftHand(highLowTerminal)
                .rightHand(highHighTerminal)
                .probabilityNaLeftHand(0.5)
                .splitRule(new NumericSplitRule((NumericCovariate) covariates.get(1), 5.0))
                .build();

        final SplitNode<Double> rootSplitNode = SplitNode.<Double>builder()
                .leftHand(lowSplitNode)
                .rightHand(highSplitNode)
                .probabilityNaLeftHand(0.5)
                .splitRule(new BooleanSplitRule((BooleanCovariate) covariates.get(0)))
                .build();

        return new Tree<>(rootSplitNode, indices);

    }

    // Experiment with random seeds to first examine what a split does so we know what to expect
    /*
    public static void main(String[] args){

        // Behaviour for OOB
        final List<Integer> ints1 = IntStream.range(5, 9).boxed().collect(Collectors.toList());
        final List<Integer> ints2 = IntStream.range(1, 5).boxed().collect(Collectors.toList());

        final Random random = new Random(123);
        Collections.shuffle(ints1, random);
        Collections.shuffle(ints2, random);

        System.out.println(ints1);
        System.out.println(ints2);
        // [5, 6, 8, 7]
        // [3, 4, 1, 2]


        // Behaviour for no-OOB
        final List<Integer> fullInts1 = IntStream.range(1, 9).boxed().collect(Collectors.toList());
        final List<Integer> fullInts2 = IntStream.range(1, 9).boxed().collect(Collectors.toList());
        final Random fullIntsRandom = new Random(123);

        Collections.shuffle(fullInts1, fullIntsRandom);
        Collections.shuffle(fullInts2, fullIntsRandom);
        System.out.println(fullInts1);
        System.out.println(fullInts2);
        // [1, 4, 8, 2, 5, 3, 7, 6]
        // [6, 1, 4, 7, 5, 2, 8, 3]

    }
    */


    private double[] difference(double[] a, double[] b){
        final double[] results = new double[a.length];

        for(int i = 0; i < a.length; i++){
            results[i] = a[i] - b[i];
        }

        return results;
    }

    private void assertDoubleEquals(double[] expected, double[] actual){
        assertEquals(expected.length, actual.length, "Lengths of arrays should be equal");

        for(int i=0; i < expected.length; i++){
            assertEquals(expected[i], actual[i], 0.0000001, "Difference at " + i);
        }

    }



    @Test
    public void testVariableImportanceOnXNoOOB(){
        // x is the BooleanCovariate

        final VariableImportanceCalculator<Double, Double> calculator = new VariableImportanceCalculator<>(
                new RegressionErrorCalculator(),
                this.forest.getTrees(),
                this.rowList,
                false
        );

        final Covariate covariate = this.covariates.get(0);

        double importance[] = calculator.calculateVariableImportanceRaw(covariate, Optional.of(new Random(123)));

        final double[] expectedBaselineError = {0.0, 4.0}; // first tree is accurate, second tree is not

        // [1, 4, 8, 2, 5, 3, 7, 6]
        final List<Double> permutedPredictionsTree1 = Utils.easyList(
                0., 110., 100., 10., 0., 110., 100., 10.
        );

        // [6, 1, 4, 7, 5, 2, 8, 3]
        // Actual: [F, F, T, T, F, F, T, T]
        // Seen:   [F, F, T, T, F, F, T, T]
        // Difference: 0 all around; random chance
        final List<Double> permutedPredictionsTree2 = Utils.easyList(
                2., 12., 102., 112., 2., 12., 102., 112.
        );

        final List<Double> observedValues = this.rowList.stream().map(Row::getResponse).collect(Collectors.toList());

        final double[] expectedError = new double[2];

        expectedError[0] = new RegressionErrorCalculator().averageError(observedValues, permutedPredictionsTree1);
        expectedError[1] = new RegressionErrorCalculator().averageError(observedValues, permutedPredictionsTree2);

        final double[] expectedVimp = difference(expectedError, expectedBaselineError);

        assertDoubleEquals(expectedVimp, importance);

        final double expectedVimpMean = (expectedVimp[0] + expectedVimp[1]) / 2.0;
        final double expectedVimpVar = (Math.pow(expectedVimp[0] - expectedVimpMean, 2) + Math.pow(expectedVimp[1] - expectedVimpMean, 2)) / 1.0;
        final double expectedVimpStandardError = Math.sqrt(expectedVimpVar / 2.0);
        final double expectedZScore = expectedVimpMean / expectedVimpStandardError;

        final double actualZScore = calculator.calculateVariableImportanceZScore(covariate, Optional.of(new Random(123)));

        assertEquals(expectedZScore, actualZScore, 0.000001, "Z scores must match");
    }



    @Test
    public void testVariableImportanceOnXOOB(){
        // x is the BooleanCovariate

        final VariableImportanceCalculator<Double, Double> calculator = new VariableImportanceCalculator<>(
                new RegressionErrorCalculator(),
                this.forest.getTrees(),
                this.rowList,
                true
        );

        final Covariate covariate = this.covariates.get(0);

        double importance[] = calculator.calculateVariableImportanceRaw(covariate, Optional.of(new Random(123)));

        final double[] expectedBaselineError = {0.0, 4.0}; // first tree is accurate, second tree is not

        // [5, 6, 8, 7]
        // Actual: [F, F, T, T]
        // Seen:   [F, F, T, T]
        // Difference: No differences
        final List<Double> permutedPredictionsTree1 = Utils.easyList(
                0., 10., 100., 110.
        );

        // [3, 4, 1, 2]
        // Actual: [F, F, T, T]
        // Seen:   [T, T, F, F]
        // Difference: +100, +100, -100, -100
        final List<Double> permutedPredictionsTree2 = Utils.easyList(
                102., 112., 2., 12.
        );

        final List<Double> observedValues = this.rowList.stream().map(Row::getResponse).collect(Collectors.toList());
        final List<Double> tree1OOBValues = observedValues.subList(4, 8);
        final List<Double> tree2OOBValues = observedValues.subList(0, 4);

        final double[] expectedError = new double[2];

        expectedError[0] = new RegressionErrorCalculator().averageError(tree1OOBValues, permutedPredictionsTree1);
        expectedError[1] = new RegressionErrorCalculator().averageError(tree2OOBValues, permutedPredictionsTree2);

        final double[] expectedVimp = difference(expectedError, expectedBaselineError);

        assertDoubleEquals(expectedVimp, importance);

        final double expectedVimpMean = (expectedVimp[0] + expectedVimp[1]) / 2.0;
        final double expectedVimpVar = (Math.pow(expectedVimp[0] - expectedVimpMean, 2) + Math.pow(expectedVimp[1] - expectedVimpMean, 2)) / 1.0;
        final double expectedVimpStandardError = Math.sqrt(expectedVimpVar / 2.0);
        final double expectedZScore = expectedVimpMean / expectedVimpStandardError;

        final double actualZScore = calculator.calculateVariableImportanceZScore(covariate, Optional.of(new Random(123)));

        assertEquals(expectedZScore, actualZScore, 0.000001, "Z scores must match");
    }


    @Test
    public void testVariableImportanceOnYNoOOB(){
        // y is the NumericCovariate

        final VariableImportanceCalculator<Double, Double> calculator = new VariableImportanceCalculator<>(
                new RegressionErrorCalculator(),
                this.forest.getTrees(),
                this.rowList,
                false
        );

        final Covariate covariate = this.covariates.get(1);

        double importance[] = calculator.calculateVariableImportanceRaw(covariate, Optional.of(new Random(123)));

        final double[] expectedBaselineError = {0.0, 4.0}; // first tree is accurate, second tree is not

        // [1, 4, 8, 2, 5, 3, 7, 6]
        // Actual:     [F, T, F, T, F, T, F, T]
        // Seen:       [F, T, T, T, F, F, F, T]
        // Difference: [=, =, +, =, =, -, =, =]x10
        final List<Double> permutedPredictionsTree1 = Utils.easyList(
                0., 10., 110., 110., 0., 0., 100., 110.
        );

        // [6, 1, 4, 7, 5, 2, 8, 3]
        // Actual:     [F, T, F, T, F, T, F, T]
        // Seen:       [T, F, T, F, F, T, T, F]
        // Difference: [+, -, +, -, =, =, +, -]
        final List<Double> permutedPredictionsTree2 = Utils.easyList(
                12., 2., 112., 102., 2., 12., 112., 102.
        );

        final List<Double> observedValues = this.rowList.stream().map(Row::getResponse).collect(Collectors.toList());

        final double[] expectedError = new double[2];

        expectedError[0] = new RegressionErrorCalculator().averageError(observedValues, permutedPredictionsTree1);
        expectedError[1] = new RegressionErrorCalculator().averageError(observedValues, permutedPredictionsTree2);

        final double[] expectedVimp = difference(expectedError, expectedBaselineError);

        assertDoubleEquals(expectedVimp, importance);

        final double expectedVimpMean = (expectedVimp[0] + expectedVimp[1]) / 2.0;
        final double expectedVimpVar = (Math.pow(expectedVimp[0] - expectedVimpMean, 2) + Math.pow(expectedVimp[1] - expectedVimpMean, 2)) / 1.0;
        final double expectedVimpStandardError = Math.sqrt(expectedVimpVar / 2.0);
        final double expectedZScore = expectedVimpMean / expectedVimpStandardError;

        final double actualZScore = calculator.calculateVariableImportanceZScore(covariate, Optional.of(new Random(123)));

        assertEquals(expectedZScore, actualZScore, 0.000001, "Z scores must match");
    }



    @Test
    public void testVariableImportanceOnYOOB(){
        // y is the NumericCovariate

        final VariableImportanceCalculator<Double, Double> calculator = new VariableImportanceCalculator<>(
                new RegressionErrorCalculator(),
                this.forest.getTrees(),
                this.rowList,
                true
        );

        final Covariate covariate = this.covariates.get(1);

        double importance[] = calculator.calculateVariableImportanceRaw(covariate, Optional.of(new Random(123)));

        final double[] expectedBaselineError = {0.0, 4.0}; // first tree is accurate, second tree is not

        // [5, 6, 8, 7]
        // Actual:     [F, T, F, T]
        // Seen:       [F, T, T, F]
        // Difference: [=, =, +, -]x10
        final List<Double> permutedPredictionsTree1 = Utils.easyList(
                0., 10., 110., 100.
        );

        // [3, 4, 1, 2]
        // Actual:     [F, T, F, T]
        // Seen:       [F, T, F, T]
        // Difference: [=, =, =, =]x10 no change
        final List<Double> permutedPredictionsTree2 = Utils.easyList(
                2., 12., 102., 112.
        );

        final List<Double> observedValues = this.rowList.stream().map(Row::getResponse).collect(Collectors.toList());
        final List<Double> tree1OOBValues = observedValues.subList(4, 8);
        final List<Double> tree2OOBValues = observedValues.subList(0, 4);

        final double[] expectedError = new double[2];

        expectedError[0] = new RegressionErrorCalculator().averageError(tree1OOBValues, permutedPredictionsTree1);
        expectedError[1] = new RegressionErrorCalculator().averageError(tree2OOBValues, permutedPredictionsTree2);

        final double[] expectedVimp = difference(expectedError, expectedBaselineError);

        assertDoubleEquals(expectedVimp, importance);

        final double expectedVimpMean = (expectedVimp[0] + expectedVimp[1]) / 2.0;
        final double expectedVimpVar = (Math.pow(expectedVimp[0] - expectedVimpMean, 2) + Math.pow(expectedVimp[1] - expectedVimpMean, 2)) / 1.0;
        final double expectedVimpStandardError = Math.sqrt(expectedVimpVar / 2.0);
        final double expectedZScore = expectedVimpMean / expectedVimpStandardError;

        final double actualZScore = calculator.calculateVariableImportanceZScore(covariate, Optional.of(new Random(123)));

        assertEquals(expectedZScore, actualZScore, 0.000001, "Z scores must match");


    }



    @Test
    public void testVariableImportanceOnZNoOOB(){
        // z is the useless FactorCovariate

        final VariableImportanceCalculator<Double, Double> calculator = new VariableImportanceCalculator<>(
                new RegressionErrorCalculator(),
                this.forest.getTrees(),
                this.rowList,
                false
        );

        final double[] importance = calculator.calculateVariableImportanceRaw(this.covariates.get(2), Optional.of(new Random(123)));
        final double[] expectedImportance = {0.0, 0.0};


        // FactorImportance did nothing; so permuting it will make no difference to baseline error
        assertDoubleEquals(expectedImportance, importance);
    }

    @Test
    public void testVariableImportanceOnZOOB(){
        // z is the useless FactorCovariate

        final VariableImportanceCalculator<Double, Double> calculator = new VariableImportanceCalculator<>(
                new RegressionErrorCalculator(),
                this.forest.getTrees(),
                this.rowList,
                true
        );

        final double[] importance = calculator.calculateVariableImportanceRaw(this.covariates.get(2), Optional.of(new Random(123)));
        final double[] expectedImportance = {0.0, 0.0};


        // FactorImportance did nothing; so permuting it will make no difference to baseline error
        assertDoubleEquals(expectedImportance, importance);
    }



}
