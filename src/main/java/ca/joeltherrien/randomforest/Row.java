package ca.joeltherrien.randomforest;


import ca.joeltherrien.randomforest.covariates.Covariate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Row<Y> extends CovariateRow {

	private final Y response;

	public Row(final Covariate.Value[] valueArray, final int id, final Y response){
	    super(valueArray, id);
	    this.response = response;
    }




	public Y getResponse() {
		return this.response;
	}
	
	@Override
	public String toString() {
		return "Row " + this.getId();
	}

	public static <Y> Row<Y> createSimple(Map<String, String> simpleMap, List<Covariate> covariateList, int id, final Y response){
		final Covariate.Value[] valueArray = new Covariate.Value[covariateList.size()];
		final Map<String, Covariate> covariateMap = new HashMap<>();

		covariateList.forEach(covariate -> covariateMap.put(covariate.getName(), covariate));

		simpleMap.forEach((name, valueStr) -> {
			final Covariate covariate = covariateMap.get(name);
			if(covariate != null){ // happens often in tests where we experiment with adding / removing covariates
				valueArray[covariate.getIndex()] = covariate.createValue(valueStr);
			}
		});

		return new Row<Y>(valueArray, id, response);
	}
	
}
