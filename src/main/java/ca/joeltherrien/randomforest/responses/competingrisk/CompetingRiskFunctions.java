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
}
