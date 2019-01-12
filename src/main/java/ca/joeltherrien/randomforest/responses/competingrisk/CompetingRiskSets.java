package ca.joeltherrien.randomforest.responses.competingrisk;

public interface CompetingRiskSets<T extends CompetingRiskResponse> {

    double[] getDistinctTimes();
    int getRiskSetLeft(int timeIndex, int event);
    int getRiskSetTotal(int timeIndex, int event);
    int getNumberOfEventsLeft(int timeIndex, int event);
    int getNumberOfEventsTotal(int timeIndex, int event);

    void update(T rowMovedToLeft);

}
