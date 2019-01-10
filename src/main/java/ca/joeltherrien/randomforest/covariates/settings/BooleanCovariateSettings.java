package ca.joeltherrien.randomforest.covariates.settings;

import ca.joeltherrien.randomforest.covariates.BooleanCovariate;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor // required by Jackson
@Data
public final class BooleanCovariateSettings extends CovariateSettings<Boolean> {

    public BooleanCovariateSettings(String name){
        super(name);
    }

    @Override
    public BooleanCovariate build(int index) {
        return new BooleanCovariate(name, index);
    }
}
