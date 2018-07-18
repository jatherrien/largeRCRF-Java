package ca.joeltherrien.randomforest;

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

public class DataLoader {

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
            final Map<String, Covariate.Value> covariateValueMap = new HashMap<>();

            for(final Covariate<?> covariate : covariates){
                covariateValueMap.put(covariate.getName(), covariate.createValue(record.get(covariate.getName())));
            }

            final Y y = responseLoader.parse(record);

            dataset.add(new Row<>(covariateValueMap, id++, y));

        }

        return dataset;

    }

    public static <O, FO> Forest<O, FO> loadForest(File folder, ResponseCombiner<O, FO> treeResponseCombiner) throws IOException, ClassNotFoundException {
        if(!folder.isDirectory()){
            throw new IllegalArgumentException("Tree directory must be a directory!");
        }

        final File[] treeFiles = folder.listFiles(((file, s) -> s.endsWith(".tree")));
        final List<File> treeFileList = Arrays.asList(treeFiles);

        Collections.sort(treeFileList, Comparator.comparing(File::getName));

        final List<Tree<O>> treeList = new ArrayList<>(treeFileList.size());

        for(final File treeFile : treeFileList){
            final ObjectInputStream inputStream = new ObjectInputStream(new GZIPInputStream(new FileInputStream(treeFile)));

            final Tree<O> tree = (Tree) inputStream.readObject();

            treeList.add(tree);

        }

        final Forest forest = Forest.<O, FO>builder()
                .trees(treeList)
                .treeResponseCombiner(treeResponseCombiner)
                .build();

        return forest;

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
