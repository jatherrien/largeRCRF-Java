package ca.joeltherrien.randomforest;


import java.util.Map;

public class Row<Y> extends CovariateRow {

	private final Y response;

	public Row(Map<String, Covariate.Value> valueMap, int id, Y response){
	    super(valueMap, id);
	    this.response = response;
    }


	public Y getResponse() {
		return this.response;
	}
	
	@Override
	public String toString() {
		return "Row " + this.getId();
	}
	
	
	
}
