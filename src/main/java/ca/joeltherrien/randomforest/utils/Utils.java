/*
 * Copyright (c) 2019 Joel Therrien.
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ca.joeltherrien.randomforest.utils;

import java.util.*;

public final class Utils {

    public static StepFunction estimateOneMinusECDF(final double[] times){
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

        return RightContinuousStepFunction.constructFromPoints(pointList, 1.0);

    }

    public static <T> void reduceListToSize(List<T> list, int n, final Random random){
        if(list.size() <= n){
            return;
        }

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

    /**
     * Returns the index of the largest (in terms of time) Point that is <= the provided time value.
     *
     * @param startIndex Only search from startIndex (inclusive)
     * @param endIndex Only search up to endIndex (exclusive)
     * @param time
     * @return The index of the largest Point who's time is <= the time parameter.
     */
    public static int binarySearchLessThan(int startIndex, int endIndex, double[] x, double time){
        final int range = endIndex - startIndex;

        if(range == 0 || x[endIndex-1] <= time){
            // we're already too far
            return endIndex - 1;
        }

        if(x[startIndex] > time){
            return -1;
        }

        if(range < 200){
            for(int i = startIndex; i < endIndex; i++){
                if(x[i] > time){
                    return i - 1;
                }
            }
        }

        // else


        final int middle = startIndex + range / 2;
        final double middleTime = x[middle];
        if(middleTime < time){
            // go right
            return binarySearchLessThan(middle, endIndex, x, time);
        }
        else if(middleTime > time){
            // go left
            return binarySearchLessThan(0, middle, x, time);
        }
        else{ // middleTime == time
            return middle;
        }
    }

    /**
     * Replacement for Java 9's List.of
     *
     * @param array
     * @param <T>
     * @return A list
     */
    public static <T> List<T> easyList(T... array){
        final List<T> list = new ArrayList<>(array.length);

        Collections.addAll(list, array);

        return list;

    }

    /**
     * Replacement for Java 9's Map.of
     *
     * @param array
     * @return A map
     */
    public static Map easyMap(Object... array){
        if(array.length % 2 != 0){
            throw new IllegalArgumentException("Must provide a value for every key");
        }

        final Map map = new HashMap();
        for(int i=0; i<array.length; i+=2){
            map.put(array[i], array[i+1]);
        }

        return map;
    }

    /**
     * Replacement for Java 9's Map.of
     * @return A map
     */
    public static <K,V> Map<K,V> easyMap(K k1, V v1){
        final Map<K,V> map = new HashMap<>();
        map.put(k1, v1);

        return map;
    }

    /**
     * Replacement for Java 9's Map.of
     * @return A map
     */
    public static <K,V> Map<K,V> easyMap(K k1, V v1, K k2, V v2){
        final Map<K,V> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);

        return map;
    }

    /**
     * Replacement for Java 9's Map.of
     * @return A map
     */
    public static <K,V> Map<K,V> easyMap(K k1, V v1, K k2, V v2, K k3, V v3){
        final Map<K,V> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);

        return map;
    }

    /**
     * Replacement for Java 9's Map.of
     * @return A map
     */
    public static <K,V> Map<K,V> easyMap(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4){
        final Map<K,V> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);

        return map;
    }

}
