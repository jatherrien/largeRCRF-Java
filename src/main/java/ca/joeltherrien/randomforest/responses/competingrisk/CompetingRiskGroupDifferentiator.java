package ca.joeltherrien.randomforest.responses.competingrisk;

import ca.joeltherrien.randomforest.tree.GroupDifferentiator;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.stream.Stream;

/**
 * See page 761 of Random survival forests for competing risks by Ishwaran et al. The class is abstract as Gray's test
 * modifies the abstract method.
 *
 */
public abstract class CompetingRiskGroupDifferentiator<Y extends CompetingResponse> implements GroupDifferentiator<Y>{

    @Override
    public abstract Double differentiate(List<Y> leftHand, List<Y> rightHand);

    abstract double riskSet(final List<Y> eventList, double time, int eventOfFocus);

    private double numberOFEventsAtTime(int eventOfFocus, List<Y> eventList, double time){
        return (double) eventList.stream()
                .filter(event -> event.getDelta() == eventOfFocus)
                .filter(event -> event.getU() == time) // since delta != 0 we know censoring didn't occur prior to this
                .count();

    }

    /**
     * Calculates the log rank value (or the Gray's test value) for a *specific* event cause.
     *
     * @param eventOfFocus
     * @param leftHand A non-empty list of CompetingResponse
     * @param rightHand A non-empty list of CompetingResponse
     * @return
     */
    LogRankValue specificLogRankValue(final int eventOfFocus, List<Y> leftHand, List<Y> rightHand){

        final double[] distinctEventTimes = Stream.concat(
                leftHand.stream(), rightHand.stream()
        )
                .filter(event -> event.getDelta() != 0) // remove censored events
                .mapToDouble(event -> event.getU())
                .distinct()
                .toArray();

        double summation = 0.0;
        double varianceSquared = 0.0;

        for(final double time_k : distinctEventTimes){
            final double weight = weight(time_k); // W_j(t_k)
            final double numberEventsAtTimeDaughterLeft = numberOFEventsAtTime(eventOfFocus, leftHand, time_k); // d_{j,l}(t_k)
            final double numberEventsAtTimeDaughterRight = numberOFEventsAtTime(eventOfFocus, rightHand, time_k); // d_{j,r}(t_k)
            final double numberOfEventsAtTime = numberEventsAtTimeDaughterLeft + numberEventsAtTimeDaughterRight; // d_j(t_k)

            final double individualsAtRiskDaughterLeft = riskSet(leftHand, time_k, eventOfFocus); // Y_l(t_k)
            final double individualsAtRiskDaughterRight = riskSet(rightHand, time_k, eventOfFocus); // Y_r(t_k)
            final double individualsAtRisk = individualsAtRiskDaughterLeft + individualsAtRiskDaughterRight; // Y(t_k)

            summation = summation + weight*(numberEventsAtTimeDaughterLeft - numberOfEventsAtTime*individualsAtRiskDaughterLeft/individualsAtRisk);

            varianceSquared = varianceSquared + weight*weight*numberOfEventsAtTime*individualsAtRiskDaughterLeft/individualsAtRisk
                    * (1.0 - individualsAtRiskDaughterLeft / individualsAtRisk)
                    * ((individualsAtRisk - numberOfEventsAtTime) / (individualsAtRisk - 1.0));


        }

        return new LogRankValue(summation, varianceSquared);
    }

    double weight(double time){
        return 1.0; // TODO - make configurable
        // A value of 1 "corresponds to the standard log-rank test which has optimal power for detecting alternatives where the cause-specific hazards are proportional"
        //TODO - look into what weights might be more appropriate.
    }

    @Data
    @AllArgsConstructor
    static class LogRankValue{
        private final double numerator;
        private final double varianceSquared;

        public double getVariance(){
            return Math.sqrt(varianceSquared);
        }
    }


}
