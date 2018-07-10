package ca.joeltherrien.randomforest.responses.competingrisk;

import lombok.Data;

/**
 * Represents a point in our estimate of either the cumulative hazard function or the cumulative incidence function.
 *
 */
@Data
public class Point {

    private final Double time;
    private final Double y;

}
