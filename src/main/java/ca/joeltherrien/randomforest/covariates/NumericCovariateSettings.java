package ca.joeltherrien.randomforest.covariates;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor // required by Jackson
@Data
public final class NumericCovariateSettings extends CovariateSettings<Double> {

    public NumericCovariateSettings(String name){
        super(name);
    }

    @Override
    public NumericCovariate build(int index) {
        return new NumericCovariate(name, index);
    }
}
