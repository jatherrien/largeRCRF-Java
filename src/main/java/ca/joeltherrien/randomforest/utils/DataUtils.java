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
import ca.joeltherrien.randomforest.tree.Forest;
import ca.joeltherrien.randomforest.tree.ResponseCombiner;
import ca.joeltherrien.randomforest.tree.Tree;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class DataUtils {

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

    public static <O, FO> Forest<O, FO> loadForest(File folder, ResponseCombiner<O, FO> treeResponseCombiner) throws IOException, ClassNotFoundException {
        if(!folder.isDirectory()){
            throw new IllegalArgumentException("Tree directory must be a directory!");
        }

        final File[] treeFiles = folder.listFiles((file, s) -> s.endsWith(".tree"));
        final List<File> treeFileList = Arrays.asList(treeFiles);

        Collections.sort(treeFileList, Comparator.comparing(File::getName));

        final List<Tree<O>> treeList = new ArrayList<>(treeFileList.size());

        for(final File treeFile : treeFileList){
            final ObjectInputStream inputStream = new ObjectInputStream(new GZIPInputStream(new FileInputStream(treeFile)));

            final Tree<O> tree = (Tree) inputStream.readObject();

            treeList.add(tree);

        }

        return Forest.<O, FO>builder()
                .trees(treeList)
                .treeResponseCombiner(treeResponseCombiner)
                .build();

    }

    public static <O, FO> Forest<O, FO> loadForest(String folder, ResponseCombiner<O, FO> treeResponseCombiner) throws IOException, ClassNotFoundException {
        final File directory = new File(folder);
        return loadForest(directory, treeResponseCombiner);
    }

    public static void saveObject(Serializable object, String filename) throws IOException {
        final ObjectOutputStream outputStream = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(filename)));
        outputStream.writeObject(object);
        outputStream.close();
    }

    public static Object loadObject(String filename) throws IOException, ClassNotFoundException {
        final ObjectInputStream inputStream = new ObjectInputStream(new GZIPInputStream(new FileInputStream(filename)));
        final Object object = inputStream.readObject();
        inputStream.close();

        return object;

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
