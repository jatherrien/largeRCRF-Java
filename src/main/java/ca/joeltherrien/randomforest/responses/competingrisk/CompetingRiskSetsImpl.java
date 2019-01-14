package ca.joeltherrien.randomforest.responses.competingrisk;

import java.util.Arrays;

public class CompetingRiskSetsImpl implements CompetingRiskSets<CompetingRiskResponse> {

    final double[] times; // length m array
    int[] riskSetLeft; // length m array
    final int[] riskSetTotal; // length m array
    int[][] numberOfEventsLeft; // J+1 x m array
    final int[][] numberOfEventsTotal; // J+1 x m array


    public CompetingRiskSetsImpl(double[] times, int[] riskSetLeft, int[] riskSetTotal, int[][] numberOfEventsLeft, int[][] numberOfEventsTotal) {
        this.times = times;
        this.riskSetLeft = riskSetLeft;
        this.riskSetTotal = riskSetTotal;
        this.numberOfEventsLeft = numberOfEventsLeft;
        this.numberOfEventsTotal = numberOfEventsTotal;
    }

    @Override
    public double[] getDistinctTimes() {
        return times;
    }

    @Override
    public int getRiskSetLeft(int timeIndex, int event) {
        return riskSetLeft[timeIndex];
    }

    @Override
    public int getRiskSetTotal(int timeIndex, int event) {
        return riskSetTotal[timeIndex];
    }


    @Override
    public int getNumberOfEventsLeft(int timeIndex, int event) {
        return numberOfEventsLeft[event][timeIndex];
    }

    @Override
    public int getNumberOfEventsTotal(int timeIndex, int event) {
        return numberOfEventsTotal[event][timeIndex];
    }

    @Override
    public void update(CompetingRiskResponse rowMovedToLeft) {
        final double time = rowMovedToLeft.getU();
        final int k = Arrays.binarySearch(times, time);

        for(int i=0; i<=k; i++){
            riskSetLeft[i]++;
        }

        numberOfEventsLeft[rowMovedToLeft.getDelta()][k]++;
    }
}
