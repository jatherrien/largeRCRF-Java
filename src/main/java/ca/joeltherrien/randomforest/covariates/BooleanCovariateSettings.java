package ca.joeltherrien.randomforest.covariates;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor // required by Jackson
@Data
public final class BooleanCovariateSettings extends CovariateSettings<Boolean> {

    public BooleanCovariateSettings(String name){
        super(name);
    }

    @Override
    BooleanCovariate build() {
        return new BooleanCovariate(name);
    }
}
