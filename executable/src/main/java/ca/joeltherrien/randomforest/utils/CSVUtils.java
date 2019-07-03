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

package ca.joeltherrien.randomforest.utils;

import ca.joeltherrien.randomforest.Row;
import ca.joeltherrien.randomforest.covariates.Covariate;
import ca.joeltherrien.randomforest.loaders.ResponseLoader;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class CSVUtils {

    public static <Y> List<Row<Y>> loadData(final List<Covariate> covariates, final ResponseLoader<Y> responseLoader, String filename) throws IOException {

        final List<Row<Y>> dataset = new ArrayList<>();

        final Reader input;
        if(filename.endsWith(".gz")){
            final FileInputStream inputStream = new FileInputStream(filename);
            final GZIPInputStream gzipInputStream = new GZIPInputStream(inputStream);

            input = new InputStreamReader(gzipInputStream);
        }
        else{
            input = new FileReader(filename);
        }


        final CSVParser parser = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(input);


        int id = 1;
        for(final CSVRecord record : parser){
            final Covariate.Value[] valueArray = new Covariate.Value[covariates.size()];

            for(final Covariate<?> covariate : covariates){
                valueArray[covariate.getIndex()] = covariate.createValue(record.get(covariate.getName()));
            }

            final Y y = responseLoader.parse(record);

            dataset.add(new Row<>(valueArray, id++, y));

        }

        return dataset;

    }

}
