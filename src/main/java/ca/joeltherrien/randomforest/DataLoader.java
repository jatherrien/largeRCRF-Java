package ca.joeltherrien.randomforest;

import ca.joeltherrien.randomforest.covariates.Covariate;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataLoader {

    public static <Y> List<Row<Y>> loadData(final List<Covariate> covariates, final ResponseLoader<Y> responseLoader, String filename) throws IOException {

        final List<Row<Y>> dataset = new ArrayList<>();

        final Reader input = new FileReader(filename);
        final CSVParser parser = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(input);


        int id = 1;
        for(final CSVRecord record : parser){
            final Map<String, Covariate.Value> covariateValueMap = new HashMap<>();

            for(final Covariate<?> covariate : covariates){
                covariateValueMap.put(covariate.getName(), covariate.createValue(record.get(covariate.getName())));
            }

            final Y y = responseLoader.parse(record);

            dataset.add(new Row<>(covariateValueMap, id++, y));

        }

        return dataset;

    }

    @FunctionalInterface
    public interface ResponseLoader<Y>{
        Y parse(CSVRecord record);
    }

    @RequiredArgsConstructor
    public static class DoubleLoader implements ResponseLoader<Double> {

        private final String yName;

        public DoubleLoader(final ObjectNode node){
            this.yName = node.get("name").asText();
        }
        @Override
        public Double parse(CSVRecord record) {
            return Double.parseDouble(record.get(yName));
        }
    }

}
