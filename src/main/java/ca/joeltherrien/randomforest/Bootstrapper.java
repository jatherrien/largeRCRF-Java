package ca.joeltherrien.randomforest;

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@RequiredArgsConstructor
public class Bootstrapper<T> {

    final private List<T> originalData;

    public List<T> bootstrap(){
        final int n = originalData.size();

        final List<T> newList = new ArrayList<>(n);

        for(int i=0; i<n; i++){
            final int index = ThreadLocalRandom.current().nextInt(n);

            newList.add(originalData.get(index));
        }

        return newList;

    }

}
