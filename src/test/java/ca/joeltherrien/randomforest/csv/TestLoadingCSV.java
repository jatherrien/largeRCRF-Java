package ca.joeltherrien.randomforest.csv;

import ca.joeltherrien.randomforest.DataLoader;
import ca.joeltherrien.randomforest.Row;
import ca.joeltherrien.randomforest.Settings;
import ca.joeltherrien.randomforest.covariates.BooleanCovariateSettings;
import ca.joeltherrien.randomforest.covariates.Covariate;
import ca.joeltherrien.randomforest.covariates.FactorCovariateSettings;
import ca.joeltherrien.randomforest.covariates.NumericCovariateSettings;
import ca.joeltherrien.randomforest.utils.Utils;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

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
                .covariates(
                        Utils.easyList(new NumericCovariateSettings("x1"),
                                new FactorCovariateSettings("x2", Utils.easyList("dog", "cat", "mouse")),
                                new BooleanCovariateSettings("x3"))
                )
                .yVarSettings(yVarSettings)
                .build();

        final List<Covariate> covariates = settings.getCovariates().stream()
                .map(cs -> cs.build()).collect(Collectors.toList());


        final DataLoader.ResponseLoader loader = settings.getResponseLoader();

        return DataLoader.loadData(covariates, loader, settings.getTrainingDataLocation());
    }

    @Test
    public void verifyLoadingNormal() throws IOException {
        final List<Row<Double>> data = loadData("src/test/resources/testCSV.csv");

        assertData(data);
    }

    @Test
    public void verifyLoadingGz() throws IOException {
        final List<Row<Double>> data = loadData("src/test/resources/testCSV.csv.gz");

        assertData(data);
    }


    private void assertData(final List<Row<Double>> data){
        assertEquals(4, data.size());

        Row<Double> row = data.get(0);
        assertEquals(5.0, (double)row.getResponse());
        assertEquals(3.0, row.getCovariateValue("x1").getValue());
        assertEquals("mouse", row.getCovariateValue("x2").getValue());
        assertEquals(true, row.getCovariateValue("x3").getValue());

        row = data.get(1);
        assertEquals(2.0, (double)row.getResponse());
        assertEquals(1.0, row.getCovariateValue("x1").getValue());
        assertEquals("dog", row.getCovariateValue("x2").getValue());
        assertEquals(false, row.getCovariateValue("x3").getValue());

        row = data.get(2);
        assertEquals(9.0, (double)row.getResponse());
        assertEquals(1.5, row.getCovariateValue("x1").getValue());
        assertEquals("cat", row.getCovariateValue("x2").getValue());
        assertEquals(true, row.getCovariateValue("x3").getValue());

        row = data.get(3);
        assertEquals(-3.0, (double)row.getResponse());
        assertTrue(row.getCovariateValue("x1").isNA());
        assertTrue(row.getCovariateValue("x2").isNA());
        assertTrue(row.getCovariateValue("x3").isNA());
    }

}
