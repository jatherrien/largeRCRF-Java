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

package ca.joeltherrien.randomforest.csv;

import ca.joeltherrien.randomforest.utils.DataUtils;
import ca.joeltherrien.randomforest.Row;
import ca.joeltherrien.randomforest.Settings;
import ca.joeltherrien.randomforest.covariates.settings.BooleanCovariateSettings;
import ca.joeltherrien.randomforest.covariates.Covariate;
import ca.joeltherrien.randomforest.covariates.settings.FactorCovariateSettings;
import ca.joeltherrien.randomforest.covariates.settings.NumericCovariateSettings;
import ca.joeltherrien.randomforest.utils.Utils;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestLoadingCSV {

    /*
        y,x1,x2,x3
        5,3.0,"mouse",true
        2,1.0,"dog",false
        9,1.5,"cat",true
        -3,NA,NA,NA
     */


    public List<Row<Double>> loadData(String filename) throws IOException {
        final ObjectNode yVarSettings = new ObjectNode(JsonNodeFactory.instance);
        yVarSettings.set("type", new TextNode("Double"));
        yVarSettings.set("name", new TextNode("y"));

        final Settings settings = Settings.builder()
                .trainingDataLocation(filename)
                .covariateSettings(
                        Utils.easyList(new NumericCovariateSettings("x1"),
                                new FactorCovariateSettings("x2", Utils.easyList("dog", "cat", "mouse")),
                                new BooleanCovariateSettings("x3"))
                )
                .yVarSettings(yVarSettings)
                .build();

        final List<Covariate> covariates = settings.getCovariates();


        final DataUtils.ResponseLoader loader = settings.getResponseLoader();

        return DataUtils.loadData(covariates, loader, settings.getTrainingDataLocation());
    }

    @Test
    public void verifyLoadingNormal(final List<Covariate> covariates) throws IOException {
        final List<Row<Double>> data = loadData("src/test/resources/testCSV.csv");

        assertData(data, covariates);
    }

    @Test
    public void verifyLoadingGz(final List<Covariate> covariates) throws IOException {
        final List<Row<Double>> data = loadData("src/test/resources/testCSV.csv.gz");

        assertData(data, covariates);
    }


    private void assertData(final List<Row<Double>> data, final List<Covariate> covariates){
        final Covariate x1 = covariates.get(0);
        final Covariate x2 = covariates.get(0);
        final Covariate x3 = covariates.get(0);

        assertEquals(4, data.size());

        Row<Double> row = data.get(0);
        assertEquals(5.0, (double)row.getResponse());
        assertEquals(3.0, row.getCovariateValue(x1).getValue());
        assertEquals("mouse", row.getCovariateValue(x2).getValue());
        assertEquals(true, row.getCovariateValue(x3).getValue());

        row = data.get(1);
        assertEquals(2.0, (double)row.getResponse());
        assertEquals(1.0, row.getCovariateValue(x1).getValue());
        assertEquals("dog", row.getCovariateValue(x2).getValue());
        assertEquals(false, row.getCovariateValue(x3).getValue());

        row = data.get(2);
        assertEquals(9.0, (double)row.getResponse());
        assertEquals(1.5, row.getCovariateValue(x1).getValue());
        assertEquals("cat", row.getCovariateValue(x2).getValue());
        assertEquals(true, row.getCovariateValue(x3).getValue());

        row = data.get(3);
        assertEquals(-3.0, (double)row.getResponse());
        assertTrue(row.getCovariateValue(x1).isNA());
        assertTrue(row.getCovariateValue(x2).isNA());
        assertTrue(row.getCovariateValue(x3).isNA());
    }

}
