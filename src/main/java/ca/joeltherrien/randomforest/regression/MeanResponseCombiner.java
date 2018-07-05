package ca.joeltherrien.randomforest.regression;

import ca.joeltherrien.randomforest.tree.ResponseCombiner;

import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * This implementation of the collector isn't great... but good enough given that I'm not planning to fully support regression trees.
 *
 * (It's not great because you'll lose accuracy as you sum up the doubles, since dividing by n is the very last step.)
 *
 */
public class MeanResponseCombiner implements ResponseCombiner<Double, MeanResponseCombiner.Container> {

    @Override
    public Double combine(List<Double> responses) {
        double size = responses.size();

        return responses.stream().mapToDouble(db -> db/size).sum();

    }

    @Override
    public Supplier<Container> supplier() {
        return () -> new Container(0 ,0);
    }

    @Override
    public BiConsumer<Container, Double> accumulator() {
        return (container, number) -> {
            container.number+=number;
            container.n++;
        };
    }

    @Override
    public BinaryOperator<Container> combiner() {
        return (c1, c2) -> {
            c1.number += c2.number;
            c1.n += c2.n;

            return c1;
        };
    }

    @Override
    public Function<Container, Double> finisher() {
        return (container) -> container.number/(double)container.n;
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Set.of(Characteristics.UNORDERED);
    }


    public static class Container{

        Container(double number, int n){
            this.number = number;
            this.n = n;
        }

        public Double number;
        public int n;

    }


}
