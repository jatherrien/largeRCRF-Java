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

package ca.joeltherrien.randomforest.competingrisk;

import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskFunctions;
import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskResponse;
import ca.joeltherrien.randomforest.responses.competingrisk.combiner.CompetingRiskResponseCombiner;
import ca.joeltherrien.randomforest.utils.StepFunction;
import org.junit.jupiter.api.Test;

import static ca.joeltherrien.randomforest.TestUtils.closeEnough;

import java.util.ArrayList;
import java.util.List;

public class TestCompetingRiskResponseCombiner {

    private CompetingRiskFunctions generateFunctions(){
        final List<CompetingRiskResponse> data = new ArrayList<>();

        data.add(new CompetingRiskResponse(1, 1.0));
        data.add(new CompetingRiskResponse(1, 1.0));
        data.add(new CompetingRiskResponse(1, 2.0));
        data.add(new CompetingRiskResponse(2, 1.5));
        data.add(new CompetingRiskResponse(2, 2.0));
        data.add(new CompetingRiskResponse(0, 1.5));
        data.add(new CompetingRiskResponse(0, 2.5));

        final CompetingRiskResponseCombiner combiner = new CompetingRiskResponseCombiner(new int[]{1,2});

        return combiner.combine(data);
    }

    @Test
    public void testCompetingRiskResponseCombiner(){
        final CompetingRiskFunctions functions = generateFunctions();

        final StepFunction survivalCurve = functions.getSurvivalCurve();

        // time = 1.0 1.5 2.0 2.5
        // surv = 0.7142857 0.5714286 0.1904762 0.1904762

        final double margin = 0.0000001;

        closeEnough(0.7142857, survivalCurve.evaluate(1.0), margin);
        closeEnough(0.5714286, survivalCurve.evaluate(1.5), margin);
        closeEnough(0.1904762, survivalCurve.evaluate(2.0), margin);
        closeEnough(0.1904762, survivalCurve.evaluate(2.5), margin);


        // Time = 1.0 1.5 2.0 2.5
        /* Cumulative hazard function. Each row for one event.
                  [,1]       [,2]       [,3]       [,4]
        [1,]  0.2857143  0.2857143  0.6190476  0.6190476
        [2,]  0.0000000  0.2000000  0.5333333  0.5333333
         */

        final StepFunction cumHaz1 = functions.getCauseSpecificHazardFunction(1);
        closeEnough(0.2857143, cumHaz1.evaluate(1.0), margin);
        closeEnough(0.2857143, cumHaz1.evaluate(1.5), margin);
        closeEnough(0.6190476, cumHaz1.evaluate(2.0), margin);
        closeEnough(0.6190476, cumHaz1.evaluate(2.5), margin);

        final StepFunction cumHaz2 = functions.getCauseSpecificHazardFunction(2);
        closeEnough(0.0, cumHaz2.evaluate(1.0), margin);
        closeEnough(0.2, cumHaz2.evaluate(1.5), margin);
        closeEnough(0.5333333, cumHaz2.evaluate(2.0), margin);
        closeEnough(0.5333333, cumHaz2.evaluate(2.5), margin);

        /* Time = 1.0 1.5 2.0 2.5
        Cumulative Incidence Curve. Each row for one event.
                  [,1]      [,2]      [,3]      [,4]
        [1,] 0.2857143 0.2857143 0.4761905 0.4761905
        [2,] 0.0000000 0.1428571 0.3333333 0.3333333
         */

        final StepFunction cic1 = functions.getCumulativeIncidenceFunction(1);
        closeEnough(0.2857143, cic1.evaluate(1.0), margin);
        closeEnough(0.2857143, cic1.evaluate(1.5), margin);
        closeEnough(0.4761905, cic1.evaluate(2.0), margin);
        closeEnough(0.4761905, cic1.evaluate(2.5), margin);

        final StepFunction cic2 = functions.getCumulativeIncidenceFunction(2);
        closeEnough(0.0, cic2.evaluate(1.0), margin);
        closeEnough(0.1428571, cic2.evaluate(1.5), margin);
        closeEnough(0.3333333, cic2.evaluate(2.0), margin);
        closeEnough(0.3333333, cic2.evaluate(2.5), margin);

    }


}
