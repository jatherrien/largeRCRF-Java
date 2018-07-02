package ca.joeltherrien.randomforest;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@RequiredArgsConstructor
public class CovariateRow {

    private final Map<String, Value> valueMap;

    @Getter
    private final int id;

    public Value<?> getCovariate(String name){
        return valueMap.get(name);

    }

    @Override
    public String toString(){
        return "CovariateRow " + this.id;
    }

}
