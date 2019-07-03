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

import ca.joeltherrien.randomforest.TestUtils;
import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskFunctions;
import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskResponse;
import ca.joeltherrien.randomforest.responses.competingrisk.combiner.CompetingRiskFunctionCombiner;
import ca.joeltherrien.randomforest.responses.competingrisk.combiner.CompetingRiskResponseCombiner;
import ca.joeltherrien.randomforest.utils.RightContinuousStepFunction;
import ca.joeltherrien.randomforest.utils.Utils;
import org.junit.jupiter.api.Test;

import java.util.List;

public class TestCompetingRiskFunctionCombiner {
    private final int[] events = new int[]{1,2};

    private CompetingRiskFunctions createFunction(List<CompetingRiskResponse> responses){
        final CompetingRiskResponseCombiner responseCombiner = new CompetingRiskResponseCombiner(events);

        return responseCombiner.combine(responses);
    }

    /* Data used in R code to compare with randomForestSRC

    data <- data.frame(u     =c(1,1,2,3,3,   2,2,3,4,4),
                   delta =c(2,1,1,1,0,   2,1,1,1,0))
     */

    @Test
    public void testFuncionCombiner(){
        final List<CompetingRiskResponse> set1 = Utils.easyList(
              new CompetingRiskResponse(2, 1.0),
              new CompetingRiskResponse(1, 1.0),
                new CompetingRiskResponse(1, 2.0),
                new CompetingRiskResponse(1, 3.0),
                new CompetingRiskResponse(0, 3.0)
        );

        final List<CompetingRiskResponse> set2 = Utils.easyList(
                new CompetingRiskResponse(2, 2.0),
                new CompetingRiskResponse(1, 2.0),
                new CompetingRiskResponse(1, 3.0),
                new CompetingRiskResponse(1, 4.0),
                new CompetingRiskResponse(0, 4.0)
        );

        final CompetingRiskFunctions fun1 = createFunction(set1);
        final CompetingRiskFunctions fun2 = createFunction(set2);

        final CompetingRiskFunctionCombiner combiner = new CompetingRiskFunctionCombiner(new int[]{1,2}, null);

        final CompetingRiskFunctions combinedFunction = combiner.combine(Utils.easyList(fun1, fun2));

        final RightContinuousStepFunction cif_1 = combinedFunction.getCumulativeIncidenceFunction(1);
        final RightContinuousStepFunction cif_2 = combinedFunction.getCumulativeIncidenceFunction(2);

        /* Result from randomForestSRC
                , , CIF.1

             [,1] [,2] [,3] [,4]
        [1,]  0.1  0.3  0.5  0.6

        , , CIF.2

             [,1] [,2] [,3] [,4]
        [1,]  0.1  0.2  0.2  0.2
         */

        TestUtils.closeEnough(0.1, cif_1.evaluate(1.0), 0.01);
        TestUtils.closeEnough(0.3, cif_1.evaluate(2.0), 0.01);
        TestUtils.closeEnough(0.5, cif_1.evaluate(3.0), 0.01);
        TestUtils.closeEnough(0.6, cif_1.evaluate(4.0), 0.01);

        TestUtils.closeEnough(0.1, cif_2.evaluate(1.0), 0.01);
        TestUtils.closeEnough(0.2, cif_2.evaluate(2.0), 0.01);
        TestUtils.closeEnough(0.2, cif_2.evaluate(3.0), 0.01);
        TestUtils.closeEnough(0.2, cif_2.evaluate(4.0), 0.01);


    }


}

/* Code to get randomForestSRC results; last tested on version 2.9.0

library(randomForestSRC)
data <- data.frame(u     =c(1,1,2,3,3,   2,2,3,4,4),
                   delta =c(2,1,1,1,0,   2,1,1,1,0),
                   x     =c(0,0,0,0,0,   1,1,1,1,1))

bootstrap.matrix <- matrix(0, nrow=nrow(data), ncol=2)
bootstrap.matrix[1:5,1] <- 1
bootstrap.matrix[6:10,2] <- 1


model.rfsrc <- rfsrc(Surv(u, delta) ~ x, data,
                     nodedepth = 0, splitrule="logrank",
                     bootstrap="by.user", samp=bootstrap.matrix,
                     ntree=2
                     )

new.data <- data.frame(x=c(1))

prediction <- predict(model.rfsrc, new.data)
prediction$cif
 */
