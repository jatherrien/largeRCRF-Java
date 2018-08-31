package ca.joeltherrien.randomforest.covariates;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Nuisance class to work with Jackson for persisting settings.
 *
 * @param <V>
 */
@NoArgsConstructor // required for Jackson
@Getter
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = BooleanCovariateSettings.class, name = "boolean"),
        @JsonSubTypes.Type(value = NumericCovariateSettings.class, name = "numeric"),
        @JsonSubTypes.Type(value = FactorCovariateSettings.class, name = "factor")
})
public abstract class CovariateSettings<V> {

    String name;

    CovariateSettings(String name){
        this.name = name;
    }

    public abstract Covariate<V> build();
}
