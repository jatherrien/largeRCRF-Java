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

    private final Covariate.Value[] valueArray;

    @Getter
    private final int id;

    public Covariate.Value<?> getCovariateValue(Covariate covariate){
        return valueArray[covariate.getIndex()];
    }

    @Override
    public String toString(){
        return "CovariateRow " + this.id;
    }

    public static CovariateRow createSimple(Map<String, String> simpleMap, List<Covariate> covariateList, int id){
        final Covariate.Value[] valueArray = new Covariate.Value[covariateList.size()];
        final Map<String, Covariate> covariateMap = new HashMap<>();

        covariateList.forEach(covariate -> covariateMap.put(covariate.getName(), covariate));

        simpleMap.forEach((name, valueStr) -> {
            final Covariate covariate = covariateMap.get(name);

            if(covariate != null){ // happens often in tests where we experiment with adding / removing covariates
                valueArray[covariate.getIndex()] = covariate.createValue(valueStr);
            }

            });

        return new CovariateRow(valueArray, id);
    }

}
