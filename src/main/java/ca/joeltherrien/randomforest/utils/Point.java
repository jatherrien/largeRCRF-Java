package ca.joeltherrien.randomforest.utils;

import lombok.Data;

import java.io.Serializable;

/**
 * Represents a point in our estimate of either the cumulative hazard function or the cumulative incidence function.
 *
 */
@Data
public class Point implements Serializable {
    private final double time;
    private final double y;
}
