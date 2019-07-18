package ca.joeltherrien.randomforest.utils;

import org.junit.jupiter.api.Test;

import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RightContinuousStepFunctionOperatorTests {


    // Idea - small and middle slightly overlap; middle and large slightly overlap.
    // small and large never overlap (i.e. small's x values always occur before large's)
    private final RightContinuousStepFunction smallNumbers;
    private final RightContinuousStepFunction middleNumbers;
    private final RightContinuousStepFunction largeNumbers;

    private final double delta = 0.0000000001;

    public RightContinuousStepFunctionOperatorTests(){
        smallNumbers = RightContinuousStepFunction.constructFromPoints(Utils.easyList(
                new Point(1.0, 1.0),
                new Point(2.0, 3.0),
                new Point(3.0, 2.0),
                new Point(4.0, 1.0)
        ), 0.0);

        middleNumbers = RightContinuousStepFunction.constructFromPoints(Utils.easyList(
                new Point(3.5, 4.0),
                new Point(4.0, 3.0),
                new Point(5.0, 2.0),
                new Point(6.0, 1.0)
        ), 5.0);

        largeNumbers = RightContinuousStepFunction.constructFromPoints(Utils.easyList(
                new Point(5.0, 5.0),
                new Point(6.0, 6.0),
                new Point(7.0, 3.0),
                new Point(8.0, 2.0)
        ), 3.0);

    }

    @Test
    public void testDifferenceNoOverlapLargeMinusSmall(){
        DoubleBinaryOperator operator = (a, b) -> a - b;

        final RightContinuousStepFunction largeSmallDifference = RightContinuousStepFunction.biOperation(
                largeNumbers,
                smallNumbers,
                operator);

        assertEquals(8, largeSmallDifference.getX().length);
        assertEquals(8, largeSmallDifference.getY().length);

        final double[] offsetTimes = {-0.1, 0.0, 0.1};

        for(int time = 1; time <= 9; time++){
            for(double offsetTime : offsetTimes){
                final double timeToEvaluateAt = (double) time + offsetTime;

                final double largeFunEvaluation = largeNumbers.evaluate(timeToEvaluateAt);
                final double smallFunEvaluation = smallNumbers.evaluate(timeToEvaluateAt);
                final double expectedDifference = operator.applyAsDouble(largeFunEvaluation, smallFunEvaluation);

                final double actualEvaluation = largeSmallDifference.evaluate(timeToEvaluateAt);

                assertEquals(expectedDifference, actualEvaluation, delta);
            }
        }
    }

    @Test
    public void testDifferenceNoOverlapSmallMinusLarge(){
        DoubleBinaryOperator operator = (a, b) -> a - b;

        final RightContinuousStepFunction smallLargeDifference = RightContinuousStepFunction.biOperation(
                smallNumbers,
                largeNumbers,
                operator);

        assertEquals(8, smallLargeDifference.getX().length);
        assertEquals(8, smallLargeDifference.getY().length);

        final double[] offsetTimes = {-0.1, 0.0, 0.1};

        for(int time = 1; time <= 9; time++){
            for(double offsetTime : offsetTimes){
                final double timeToEvaluateAt = (double) time + offsetTime;

                final double smallFunEvaluation = smallNumbers.evaluate(timeToEvaluateAt);
                final double largeFunEvaluation = largeNumbers.evaluate(timeToEvaluateAt);
                final double expectedDifference = operator.applyAsDouble(smallFunEvaluation, largeFunEvaluation);

                final double actualEvaluation = smallLargeDifference.evaluate(timeToEvaluateAt);

                assertEquals(expectedDifference, actualEvaluation, delta);
            }
        }
    }

    @Test
    public void testDifferenceSomeOverlapLargeMinusMiddle(){
        DoubleBinaryOperator operator = (a, b) -> a - b;

        final RightContinuousStepFunction combinedFunction = RightContinuousStepFunction.biOperation(
                largeNumbers,
                middleNumbers,
                operator);

        assertEquals(6, combinedFunction.getX().length);
        assertEquals(6, combinedFunction.getY().length);

        final double[] offsetTimes = {-0.1, 0.0, 0.1};

        for(int time = 1; time <= 9; time++){
            for(double offsetTime : offsetTimes){
                final double timeToEvaluateAt = (double) time + offsetTime;

                final double middleFunEvaluation = middleNumbers.evaluate(timeToEvaluateAt);
                final double largeFunEvaluation = largeNumbers.evaluate(timeToEvaluateAt);
                final double expectedDifference = operator.applyAsDouble(largeFunEvaluation, middleFunEvaluation);

                final double actualEvaluation = combinedFunction.evaluate(timeToEvaluateAt);

                assertEquals(expectedDifference, actualEvaluation, delta);
            }
        }
    }

    @Test
    public void testDifferenceCompleteOverlap(){
        DoubleBinaryOperator operator = (a, b) -> a - b;

        final RightContinuousStepFunction combinedFunction = RightContinuousStepFunction.biOperation(
                middleNumbers,
                middleNumbers,
                operator);

        assertEquals(4, combinedFunction.getX().length);
        assertEquals(4, combinedFunction.getY().length);

        final double[] offsetTimes = {-0.1, 0.0, 0.1};

        for(int time = 1; time <= 9; time++){
            for(double offsetTime : offsetTimes){
                final double timeToEvaluateAt = (double) time + offsetTime;

                final double actualEvaluation = combinedFunction.evaluate(timeToEvaluateAt);

                assertEquals(0.0, actualEvaluation, delta);
            }
        }
    }

    @Test
    public void testPowerFunction(){
        final DoubleUnaryOperator operator = d -> d*d;

        final RightContinuousStepFunction squaredFunction = smallNumbers.unaryOperation(operator);

        assertEquals(4, squaredFunction.getX().length);
        assertEquals(4, squaredFunction.getY().length);

        final double[] offsetTimes = {-0.1, 0.0, 0.1};

        for(int time = 1; time <= 9; time++){
            for(double offsetTime : offsetTimes){
                final double timeToEvaluateAt = (double) time + offsetTime;

                final double expectedEvaluation = operator.applyAsDouble(smallNumbers.evaluate(timeToEvaluateAt));
                final double actualEvaluation = squaredFunction.evaluate(timeToEvaluateAt);

                assertEquals(expectedEvaluation, actualEvaluation, delta);
            }
        }
    }

}
