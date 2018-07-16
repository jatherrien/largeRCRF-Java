package ca.joeltherrien.randomforest.responses.competingrisk;

import lombok.Getter;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Represents a function represented by discrete points. We assume that the function is a stepwise continuous function,
 * constant at the value of the previous encountered point.
 *
 */
public class MathFunction {

    @Getter
    private final List<Point> points;

    /**
     * Represents the value that should be returned by evaluate if there are points prior to the time the function is being evaluated at.
     *
     * Map be null.
     */
    private final Point defaultValue;

    public MathFunction(final List<Point> points){
        this(points, new Point(0.0, 0.0));
    }

    public MathFunction(final List<Point> points, final Point defaultValue){
        this.points = Collections.unmodifiableList(points);
        this.defaultValue = defaultValue;
    }

    public Point evaluate(double time){
        final Optional<Point> pointOptional = points.stream()
                .filter(point -> point.getTime() <= time)
                .max(Comparator.comparingDouble(Point::getTime));

        return pointOptional.orElse(defaultValue);

    }

    @Override
    public String toString(){
        final StringBuilder builder = new StringBuilder();
        builder.append("Default point: ");
        builder.append(defaultValue);
        builder.append("\n");

        for(final Point point : points){
            builder.append(point);
            builder.append("\n");
        }

        return builder.toString();
    }

}
