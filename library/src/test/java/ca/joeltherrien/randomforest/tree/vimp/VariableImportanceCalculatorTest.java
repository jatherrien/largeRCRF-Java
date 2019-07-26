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

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
        final List<Integer> ints1 = IntStream.range(1, 9).boxed().collect(Collectors.toList());
        final List<Integer> ints2 = IntStream.range(1, 9).boxed().collect(Collectors.toList());

        final Random random = new Random(123);
        Collections.shuffle(ints1, random);
        Collections.shuffle(ints2, random);

        System.out.println(ints1);
        // [1, 4, 8, 2, 5, 3, 7, 6]

        System.out.println(ints2);
        [6, 1, 4, 7, 5, 2, 8, 3]
    }
     */

    @Test
    public void testVariableImportanceOnXNoOOB(){
        // x is the BooleanCovariate

        Random random = new Random(123);
        final VariableImportanceCalculator<Double, Double> calculator = new VariableImportanceCalculator<>(
                new RegressionErrorCalculator(),
                this.forest,
                this.rowList,
                false
        );

        double importance = calculator.calculateVariableImportance(this.covariates.get(0), Optional.of(random));

        final double expectedBaselineError = 1.0; // Everything is off by 1, so average is 1.0

        final List<Double> permutedPredictions = Utils.easyList(
                1., 111., 101., 11., 1., 111., 101., 11.
        );
        final List<Double> observedValues = this.rowList.stream().map(Row::getResponse).collect(Collectors.toList());

        final double expectedError = new RegressionErrorCalculator().averageError(observedValues, permutedPredictions);

        assertEquals(expectedError - expectedBaselineError, importance, 0.0000001);
    }

    @Test
    public void testVariableImportanceOnXOOB(){
        // x is the BooleanCovariate

        Random random = new Random(123);
        final VariableImportanceCalculator<Double, Double> calculator = new VariableImportanceCalculator<>(
                new RegressionErrorCalculator(),
                this.forest,
                this.rowList,
                true
        );

        double importance = calculator.calculateVariableImportance(this.covariates.get(0), Optional.of(random));

        // First 4 observations are off by 2, last 4 are off by 0
        final double expectedBaselineError = 2.0*2.0 * 4.0 / 8.0;

        // Remember we are working with OOB predictions
        final List<Double> permutedPredictions = Utils.easyList(
                2., 112., 102., 12., 0., 110., 100., 10.
        );
        final List<Double> observedValues = this.rowList.stream().map(Row::getResponse).collect(Collectors.toList());

        final double expectedError = new RegressionErrorCalculator().averageError(observedValues, permutedPredictions);

        assertEquals(expectedError - expectedBaselineError, importance, 0.0000001);
    }

    @Test
    public void testVariableImportanceOnYNoOOB(){
        // y is the NumericCovariate

        Random random = new Random(123);
        final VariableImportanceCalculator<Double, Double> calculator = new VariableImportanceCalculator<>(
                new RegressionErrorCalculator(),
                this.forest,
                this.rowList,
                false
        );

        double importance = calculator.calculateVariableImportance(this.covariates.get(1), Optional.of(random));

        final double expectedBaselineError = 1.0; // Everything is off by 1, so average is 1.0

        final List<Double> permutedPredictions = Utils.easyList(
                1., 11., 111., 111., 1., 1., 101., 111.
        );
        final List<Double> observedValues = this.rowList.stream().map(Row::getResponse).collect(Collectors.toList());

        final double expectedError = new RegressionErrorCalculator().averageError(observedValues, permutedPredictions);

        assertEquals(expectedError - expectedBaselineError, importance, 0.0000001);
    }

    @Test
    public void testVariableImportanceOnYOOB(){
        // y is the NumericCovariate

        Random random = new Random(123);
        final VariableImportanceCalculator<Double, Double> calculator = new VariableImportanceCalculator<>(
                new RegressionErrorCalculator(),
                this.forest,
                this.rowList,
                true
        );

        double importance = calculator.calculateVariableImportance(this.covariates.get(1), Optional.of(random));

        // First 4 observations are off by 2, last 4 are off by 0
        final double expectedBaselineError = 2.0*2.0 * 4.0 / 8.0;

        // Remember we are working with OOB predictions
        final List<Double> permutedPredictions = Utils.easyList(
                2., 12., 112., 112., 0., 0., 100., 110.
        );
        final List<Double> observedValues = this.rowList.stream().map(Row::getResponse).collect(Collectors.toList());

        final double expectedError = new RegressionErrorCalculator().averageError(observedValues, permutedPredictions);

        assertEquals(expectedError - expectedBaselineError, importance, 0.0000001);
    }

    @Test
    public void testVariableImportanceOnZNoOOB(){
        // z is the useless FactorCovariate

        Random random = new Random(123);
        final VariableImportanceCalculator<Double, Double> calculator = new VariableImportanceCalculator<>(
                new RegressionErrorCalculator(),
                this.forest,
                this.rowList,
                false
        );

        double importance = calculator.calculateVariableImportance(this.covariates.get(2), Optional.of(random));

        // FactorImportance did nothing; so permuting it will make no difference to baseline error
        assertEquals(0, importance, 0.0000001);
    }

    @Test
    public void testVariableImportanceOnZOOB(){
        // z is the useless FactorCovariate

        Random random = new Random(123);
        final VariableImportanceCalculator<Double, Double> calculator = new VariableImportanceCalculator<>(
                new RegressionErrorCalculator(),
                this.forest,
                this.rowList,
                true
        );

        double importance = calculator.calculateVariableImportance(this.covariates.get(2), Optional.of(random));

        // FactorImportance did nothing; so permuting it will make no difference to baseline error
        assertEquals(0, importance, 0.0000001);
    }

    @Test
    public void testVariableImportanceMultiple(){
        Random random = new Random(123);
        final VariableImportanceCalculator<Double, Double> calculator = new VariableImportanceCalculator<>(
                new RegressionErrorCalculator(),
                this.forest,
                this.rowList,
                false
        );

        double importance[] = calculator.calculateVariableImportance(covariates, Optional.of(random));

        final double expectedBaselineError = 1.0; // Everything is off by 1, so average is 1.0

        final List<Double> observedValues = this.rowList.stream().map(Row::getResponse).collect(Collectors.toList());

        final List<Double> permutedPredictionsX = Utils.easyList(
                1., 111., 101., 11., 1., 111., 101., 11.
        );

        // [6, 1, 4, 7, 5, 2, 8, 3]
        final List<Double> permutedPredictionsY = Utils.easyList(
                11., 1., 111., 101., 1., 11., 111., 101.
        );

        final double expectedErrorX = new RegressionErrorCalculator().averageError(observedValues, permutedPredictionsX);
        final double expectedErrorY = new RegressionErrorCalculator().averageError(observedValues, permutedPredictionsY);

        assertEquals(expectedErrorX - expectedBaselineError, importance[0], 0.0000001);
        assertEquals(expectedErrorY - expectedBaselineError, importance[1], 0.0000001);
        assertEquals(0, importance[2], 0.0000001);

    }

}
