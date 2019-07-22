package ca.joeltherrien.randomforest.responses.competingrisk;

import ca.joeltherrien.randomforest.utils.RightContinuousStepFunction;

import java.util.Optional;

/**
 * Used to calculate the Integrated Brier Score. See Section 4.2 of "Random survival forests for competing risks" by Ishwaran.
 *
 */
public class IBSCalculator {

    private final Optional<RightContinuousStepFunction> censoringDistribution;

    public IBSCalculator(RightContinuousStepFunction censoringDistribution){
        this.censoringDistribution = Optional.of(censoringDistribution);
    }

    public IBSCalculator(){
        this.censoringDistribution = Optional.empty();
    }

    public IBSCalculator(Optional<RightContinuousStepFunction> censoringDistribution){
        this.censoringDistribution = censoringDistribution;
    }

    public double calculateError(CompetingRiskResponse response, RightContinuousStepFunction cif, int eventOfInterest, double integrationUpperBound){

        // return integral of weights*(I(response.getU() <= times & response.getDelta() == eventOfInterest) - cif(times))^2
        // Note that if we don't have weights, just treat them all as one (i.e. don't bother multiplying)

        RightContinuousStepFunction functionToIntegrate = cif;

        if(response.getDelta() == eventOfInterest){
            final RightContinuousStepFunction observedFunction = new RightContinuousStepFunction(new double[]{response.getU()}, new double[]{1.0}, 0.0);
            functionToIntegrate = RightContinuousStepFunction.biOperation(observedFunction, functionToIntegrate, (a, b) -> (a - b) * (a - b));
        } else{
            functionToIntegrate = functionToIntegrate.unaryOperation(a -> a*a);
        }

        if(censoringDistribution.isPresent()){
            final RightContinuousStepFunction weights = calculateWeights(response, censoringDistribution.get());
            functionToIntegrate = RightContinuousStepFunction.biOperation(weights, functionToIntegrate, (a, b) -> a*b);

            // the censoring weights go to 0 after the response is censored, so we can speed up results by only integrating
            // prior to the censor times
            if(response.isCensored()){
                integrationUpperBound = Math.min(integrationUpperBound, response.getU());
            }

        }

        return functionToIntegrate.integrate(0.0, integrationUpperBound);
    }

    private RightContinuousStepFunction calculateWeights(CompetingRiskResponse response, RightContinuousStepFunction censoringDistribution){
        final double recordedTime = response.getU();

        // Function(t) = firstPart(t) + secondPart(t)/thirdPart(t) where:
            // firstPart(t) = I(recordedTime <= t & !response.isCensored()) / censoringDistribution.evaluate(recordedTime);
            // secondPart(t) = I(recordedTime > t) = 1 - I(recordedTime <= t)
            // thirdPart(t) = censoringDistribution.evaluate(t)

        final RightContinuousStepFunction secondPart = new RightContinuousStepFunction(new double[]{recordedTime}, new double[]{0.0}, 1.0);
        RightContinuousStepFunction result = RightContinuousStepFunction.biOperation(secondPart, censoringDistribution,
                (second, third) -> second / third);

        if(!response.isCensored()){
            final RightContinuousStepFunction firstPart = new RightContinuousStepFunction(
                    new double[]{recordedTime},
                    new double[]{1.0 / censoringDistribution.evaluate(recordedTime)},
                    0.0);

            result = RightContinuousStepFunction.biOperation(firstPart, result, Double::sum);
        }

       return result;

    }


}
