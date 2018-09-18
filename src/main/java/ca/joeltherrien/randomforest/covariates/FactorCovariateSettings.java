package ca.joeltherrien.randomforest.covariates;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor // required by Jackson
@Data
public final class FactorCovariateSettings extends CovariateSettings<String> {

    private List<String> levels;

    public FactorCovariateSettings(String name, List<String> levels){
        super(name);
        this.levels = new ArrayList<>(levels); // Jackson struggles with List.of(...)
    }

    @Override
    public FactorCovariate build(int index) {
        return new FactorCovariate(name, index, levels);
    }
}
