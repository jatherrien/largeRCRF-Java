package ca.joeltherrien.randomforest.csv;

import ca.joeltherrien.randomforest.Main;
import ca.joeltherrien.randomforest.Row;
import ca.joeltherrien.randomforest.Settings;
import ca.joeltherrien.randomforest.covariates.BooleanCovariateSettings;
import ca.joeltherrien.randomforest.covariates.Covariate;
import ca.joeltherrien.randomforest.covariates.FactorCovariateSettings;
import ca.joeltherrien.randomforest.covariates.NumericCovariateSettings;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestLoadingCSV {

    /*
        y,x1,x2,x3
        5,3.0,"mouse",true
        2,1.0,"dog",false
        9,1.5,"cat",true
     */

    @Test
    public void verifyLoading() throws IOException {
        final Settings settings = Settings.builder()
                .dataFileLocation("src/test/resources/testCSV.csv")
                .covariates(
                        List.of(new NumericCovariateSettings("x1"),
                                new FactorCovariateSettings("x2", List.of("dog", "cat", "mouse")),
                                new BooleanCovariateSettings("x3"))
                )
                .yVar("y")
                .build();

        final List<Covariate> covariates = settings.getCovariates().stream()
                .map(cs -> cs.build()).collect(Collectors.toList());

        final List<Row<Double>> data = Main.loadData(covariates, settings);

        assertEquals(3, data.size());

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

    }

}
