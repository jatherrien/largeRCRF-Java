package ca.joeltherrien.randomforest.responses.competingrisk;

import ca.joeltherrien.randomforest.utils.MathFunction;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * Represents a response from CompetingRiskUtils#calculateGraySetsEfficiently
 *
 */
@Builder
@Getter
public class CompetingRiskGraySetsImpl implements CompetingRiskSets{

    private final List<Double> eventTimes;
    private final MathFunction[] riskSet;
    private final Map<Double, int[]> numberOfEvents;

    @Override
    public MathFunction getRiskSet(int event){
        return(riskSet[event-1]);
    }

    @Override
    public int getNumberOfEvents(Double time, int event){
        if(numberOfEvents.containsKey(time)){
            return numberOfEvents.get(time)[event];
        }

        return 0;
    }


}
