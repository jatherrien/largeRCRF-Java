package ca.joeltherrien.randomforest.responses.competingrisk;

import ca.joeltherrien.randomforest.utils.RightContinuousStepFunction;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.util.List;

@Builder
public class CompetingRiskFunctions implements Serializable {

    private final List<RightContinuousStepFunction> causeSpecificHazards;
    private final List<RightContinuousStepFunction> cumulativeIncidenceCurves;

    @Getter
    private final RightContinuousStepFunction survivalCurve;

    public RightContinuousStepFunction getCauseSpecificHazardFunction(int cause){
        return causeSpecificHazards.get(cause-1);
    }

    public RightContinuousStepFunction getCumulativeIncidenceFunction(int cause) {
        return cumulativeIncidenceCurves.get(cause-1);
    }

    public double calculateEventSpecificMortality(final int event, final double tau){
        final RightContinuousStepFunction cif = getCumulativeIncidenceFunction(event);

        double summation = 0.0;

        Double previousTime = null;
        Double previousY = null;

        final double[] cifTimes = cif.getX();
        for(int i=0; i<cifTimes.length; i++){
            final double time = cifTimes[i];

            if(time > tau){
                break;
            }

            if(previousTime != null){
                summation += previousY * (time - previousTime);
            }
            previousTime = time;
            previousY = cif.evaluateByIndex(i);
        }

        // this is to ensure that we integrate over the proper range
        if(previousTime != null){
            summation += cif.evaluate(tau) * (tau - previousTime);
        }

        return summation;

    }
}
