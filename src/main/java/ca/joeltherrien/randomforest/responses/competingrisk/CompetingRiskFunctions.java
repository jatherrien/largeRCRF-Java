package ca.joeltherrien.randomforest.responses.competingrisk;

import ca.joeltherrien.randomforest.utils.MathFunction;
import ca.joeltherrien.randomforest.utils.Point;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.util.Map;

@Builder
public class CompetingRiskFunctions implements Serializable {

    private final Map<Integer, MathFunction> causeSpecificHazardFunctionMap;
    private final Map<Integer, MathFunction> cumulativeIncidenceFunctionMap;

    @Getter
    private final MathFunction survivalCurve;

    public MathFunction getCauseSpecificHazardFunction(int cause){
        return causeSpecificHazardFunctionMap.get(cause);
    }

    public MathFunction getCumulativeIncidenceFunction(int cause) {
        return cumulativeIncidenceFunctionMap.get(cause);
    }

    public double calculateEventSpecificMortality(final int event, final double tau){
        final MathFunction cif = getCumulativeIncidenceFunction(event);

        double summation = 0.0;
        Point previousPoint = null;

        for(final Point point : cif.getPoints()){
            if(point.getTime() > tau){
                break;
            }

            if(previousPoint != null){
                summation += previousPoint.getY() * (point.getTime() - previousPoint.getTime());
            }
            previousPoint = point;

        }

        // this is to ensure that we integrate over the proper range
        if(previousPoint != null){
            summation += cif.evaluate(tau).getY() * (tau - previousPoint.getTime());
        }


        return summation;

    }
}
