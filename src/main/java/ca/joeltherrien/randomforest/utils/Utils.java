package ca.joeltherrien.randomforest.utils;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

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

    public static <T> void reduceListToSize(List<T> list, int n){
        if(list.size() <= n){
            return;
        }

        final Random random = ThreadLocalRandom.current();
        if(n > list.size()/2){
            // faster to randomly remove items
            while(list.size() > n){
                final int indexToRemove = random.nextInt(list.size());
                list.remove(indexToRemove);
            }
        }
        else{
            // Faster to create a new list
            final List<T> newList = new ArrayList<>(n);
            while(newList.size() < n){
                final int indexToAdd = random.nextInt(list.size());
                newList.add(list.remove(indexToAdd));
            }

            list.clear();
            list.addAll(newList);

        }
    }


}
