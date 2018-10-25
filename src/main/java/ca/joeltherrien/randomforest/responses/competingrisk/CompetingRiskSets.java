package ca.joeltherrien.randomforest.responses.competingrisk;

import ca.joeltherrien.randomforest.utils.MathFunction;

import java.util.List;

public interface CompetingRiskSets {

    MathFunction getRiskSet(int event);
    int getNumberOfEvents(Double time, int event);
    List<Double> getEventTimes();

}
