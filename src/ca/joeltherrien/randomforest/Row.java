package ca.joeltherrien.randomforest;

import java.util.Map;

public class Row<Y> {
	
	private final Map<String, Value> covariates;
	private final Y response;
	private final int id;
	
	public Row(Map<String, Value> covariates, Y response, int id) {
		super();
		this.covariates = covariates;
		this.response = response;
		this.id = id;
	}
	
	public Value getCovariate(String name) {
		return this.covariates.get(name);
	}
	
	public Y getResponse() {
		return this.response;
	}
	
	@Override
	public String toString() {
		return "Row " + this.id;
	}
	
	
	
}
