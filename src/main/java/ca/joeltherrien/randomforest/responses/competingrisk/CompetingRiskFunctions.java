package ca.joeltherrien.randomforest.responses.competingrisk;

import ca.joeltherrien.randomforest.utils.StepFunction;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.util.List;

@Builder
public class CompetingRiskFunctions implements Serializable {

    private final List<StepFunction> causeSpecificHazards;
    private final List<StepFunction> cumulativeIncidenceCurves;

    @Getter
    private final StepFunction survivalCurve;

    public StepFunction getCauseSpecificHazardFunction(int cause){
        return causeSpecificHazards.get(cause-1);
    }

    public StepFunction getCumulativeIncidenceFunction(int cause) {
        return cumulativeIncidenceCurves.get(cause-1);
    }

    public double calculateEventSpecificMortality(final int event, final double tau){
        final StepFunction cif = getCumulativeIncidenceFunction(event);

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
