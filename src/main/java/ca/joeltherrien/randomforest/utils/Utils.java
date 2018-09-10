package ca.joeltherrien.randomforest.utils;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public final class Utils {

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
