package ca.joeltherrien.randomforest.utils;

import java.util.*;

public class Utils {

    public static MathFunction estimateOneMinusECDF(final double[] times){
        final Point defaultPoint = new Point(0.0, 1.0);
        Arrays.sort(times);

        final Map<Double, Integer> timeCounterMap = new HashMap<>();

        for(final double time : times){
            Integer existingCount =  timeCounterMap.get(time);
            existingCount = existingCount != null ? existingCount : 0;

            timeCounterMap.put(time, existingCount+1);
        }

        final List<Map.Entry<Double, Integer>> timeCounterList = new ArrayList<>(timeCounterMap.entrySet());
        Collections.sort(timeCounterList, Comparator.comparingDouble(Map.Entry::getKey));

        final List<Point> pointList = new ArrayList<>(timeCounterList.size());

        int previousCount = times.length;
        final double n = times.length;

        for(final Map.Entry<Double, Integer> entry : timeCounterList){
            final int newCount = previousCount - entry.getValue();
            previousCount = newCount;

            pointList.add(new Point(entry.getKey(), (double) newCount / n));
        }

        return new MathFunction(pointList, defaultPoint);

    }


}
