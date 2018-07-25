package ca.joeltherrien.randomforest.responses.competingrisk;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

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
        final MathFunction cif = getCauseSpecificHazardFunction(event);

        double summation = 0.0;
        Point previousPoint = null;

        for(final Point point : cif.getPoints()){
            if(previousPoint != null){
                summation += previousPoint.getY() * (point.getTime() - previousPoint.getTime());
            }
            previousPoint = point;

        }

        // this is to ensure that we integrate over the same range for every function and get comparable results.
        // Don't need to assert whether previousPoint is null or not; if it is null then the MathFunction was incorrectly made as there will always be at least one point for a response
        summation += previousPoint.getY() * (tau - previousPoint.getTime());

        return summation;

    }
}
