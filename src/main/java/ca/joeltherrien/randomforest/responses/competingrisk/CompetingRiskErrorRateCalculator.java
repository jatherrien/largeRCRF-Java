/*
 * Copyright (c) 2019 Joel Therrien.
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ca.joeltherrien.randomforest.responses.competingrisk;

import ca.joeltherrien.randomforest.Row;
import ca.joeltherrien.randomforest.tree.Forest;
import ca.joeltherrien.randomforest.utils.StepFunction;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Based on the naive version in Section 3.2 of "Concordance for Prognastic Models with Competing Risks" by Wolbers et al.
 *
 * Note that this is the same version implemented in randomForestSRC. The downsides of this approach is that we can expect the errors to be biased, possibly severely.
 * Therefore I suggest that this measure only be used in comparing models, but not as a final output.
 */
public class CompetingRiskErrorRateCalculator {

    private final List<Row<CompetingRiskResponse>> dataset;
    private final List<CompetingRiskFunctions> riskFunctions;

    public CompetingRiskErrorRateCalculator(final List<Row<CompetingRiskResponse>> dataset, final Forest<?, CompetingRiskFunctions> forest, boolean useBootstrapPredictions){
        this.dataset = dataset;
        if(useBootstrapPredictions){
            this.riskFunctions = dataset.stream()
                    .map(forest::evaluateOOB)
                    .collect(Collectors.toList());
        }
        else{
            this.riskFunctions = forest.evaluate(dataset);
        }

    }


    public double[] calculateConcordance(final int[] events){
        final double tau = dataset.stream().mapToDouble(row -> row.getResponse().getU()).max().orElse(0.0);

        return calculateConcordance(events, tau);
    }

    private double[] calculateConcordance(final int[] events, final double tau){

        final double[] errorRates = new double[events.length];

        final List<CompetingRiskResponse> responses = dataset.stream().map(Row::getResponse).collect(Collectors.toList());

        // Let \tau be the max time.

        for(int e=0; e<events.length; e++){
            final int event = events[e];

            final double[] mortalityList = riskFunctions.stream()
                    .mapToDouble(riskFunction -> riskFunction.calculateEventSpecificMortality(event, tau))
                    .toArray();

            final double concordance = CompetingRiskUtils.calculateConcordance(responses, mortalityList, event);
            errorRates[e] = 1.0 - concordance;

        }

        return errorRates;

    }

    public double[] calculateIPCWConcordance(final int[] events, final StepFunction censoringDistribution){
        final double tau = dataset.stream().mapToDouble(row -> row.getResponse().getU()).max().orElse(0.0);

        return calculateIPCWConcordance(events, censoringDistribution, tau);
    }

    private double[] calculateIPCWConcordance(final int[] events, final StepFunction censoringDistribution, final double tau){

        final double[] errorRates = new double[events.length];

        final List<CompetingRiskResponse> responses = dataset.stream().map(Row::getResponse).collect(Collectors.toList());

        // Let \tau be the max time.

        for(int e=0; e<events.length; e++){
            final int event = events[e];

            final double[] mortalityList = riskFunctions.stream()
                    .mapToDouble(riskFunction -> riskFunction.calculateEventSpecificMortality(event, tau))
                    .toArray();

            final double concordance = CompetingRiskUtils.calculateIPCWConcordance(responses, mortalityList, event, censoringDistribution);
            errorRates[e] = 1.0 - concordance;

        }

        return errorRates;

    }



}
