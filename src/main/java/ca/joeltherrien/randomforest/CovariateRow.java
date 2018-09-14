package ca.joeltherrien.randomforest;

import ca.joeltherrien.randomforest.covariates.Covariate;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class CovariateRow implements Serializable {

    private final Map<String, Covariate.Value> valueMap;

    @Getter
    private final int id;

    public Covariate.Value<?> getCovariateValue(String name){
        return valueMap.get(name);

    }

    @Override
    public String toString(){
        return "CovariateRow " + this.id;
    }

    public static CovariateRow createSimple(Map<String, String> simpleMap, List<Covariate> covariateList, int id){
        final Map<String, Covariate.Value> valueMap = new HashMap<>();
        final Map<String, Covariate> covariateMap = new HashMap<>();

        covariateList.forEach(covariate -> covariateMap.put(covariate.getName(), covariate));

        simpleMap.forEach((name, valueStr) -> {
            if(covariateMap.containsKey(name)){
                valueMap.put(name, covariateMap.get(name).createValue(valueStr));
            }
            });

        return new CovariateRow(valueMap, id);
    }

}
