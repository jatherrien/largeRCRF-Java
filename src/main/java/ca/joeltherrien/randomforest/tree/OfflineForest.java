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

package ca.joeltherrien.randomforest.tree;

import ca.joeltherrien.randomforest.CovariateRow;
import ca.joeltherrien.randomforest.utils.IterableOfflineTree;
import lombok.AllArgsConstructor;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@AllArgsConstructor
public class OfflineForest<O, FO> extends Forest<O, FO> {

    private final File[] treeFiles;
    private final ForestResponseCombiner<O, FO> treeResponseCombiner;

    public OfflineForest(File treeDirectoryPath, ForestResponseCombiner<O, FO> treeResponseCombiner){
        this.treeResponseCombiner = treeResponseCombiner;

        if(!treeDirectoryPath.isDirectory()){
            throw new IllegalArgumentException("treeDirectoryPath must point to a directory!");
        }

        this.treeFiles = treeDirectoryPath.listFiles((file, s) -> s.endsWith(".tree"));
    }

    @Override
    public FO evaluate(CovariateRow row) {
        final List<O> predictedOutputs = new ArrayList<>(treeFiles.length);
        for(final Tree<O> tree : getTrees()){
            final O prediction = tree.evaluate(row);
            predictedOutputs.add(prediction);
        }

        return treeResponseCombiner.combine(predictedOutputs);
    }

    @Override
    public FO evaluateOOB(CovariateRow row) {
        final List<O> predictedOutputs = new ArrayList<>(treeFiles.length);
        for(final Tree<O> tree : getTrees()){
            if(!tree.idInBootstrapSample(row.getId())){
                final O prediction = tree.evaluate(row);
                predictedOutputs.add(prediction);
            }
        }

        return treeResponseCombiner.combine(predictedOutputs);
    }


    @Override
    public List<FO> evaluate(List<? extends CovariateRow> rowList){
        final List<IntermediateCombinedResponse<O, FO>> intermediatePredictions =
                IntStream.range(0, rowList.size())
                .mapToObj(i -> treeResponseCombiner.startIntermediateCombinedResponse(treeFiles.length))
                .collect(Collectors.toList());

        final Iterator<Tree<O>> treeIterator = getTrees().iterator();
        for(int treeId = 0; treeId < treeFiles.length; treeId++){
            final Tree<O> currentTree = treeIterator.next();

            IntStream.range(0, rowList.size()).parallel().forEach(
                    rowId -> {
                        final CovariateRow row = rowList.get(rowId);
                        final O prediction = currentTree.evaluate(row);
                        intermediatePredictions.get(rowId).processNewInput(prediction);
                    }
            );
        }

        return intermediatePredictions.stream().parallel()
                .map(intPred -> intPred.transformToOutput())
                .collect(Collectors.toList());
    }

    @Override
    public List<FO> evaluateSerial(List<? extends CovariateRow> rowList){
        final List<IntermediateCombinedResponse<O, FO>> intermediatePredictions =
                IntStream.range(0, rowList.size())
                        .mapToObj(i -> treeResponseCombiner.startIntermediateCombinedResponse(treeFiles.length))
                        .collect(Collectors.toList());

        final Iterator<Tree<O>> treeIterator = getTrees().iterator();
        for(int treeId = 0; treeId < treeFiles.length; treeId++){
            final Tree<O> currentTree = treeIterator.next();

            IntStream.range(0, rowList.size()).sequential().forEach(
                    rowId -> {
                        final CovariateRow row = rowList.get(rowId);
                        final O prediction = currentTree.evaluate(row);
                        intermediatePredictions.get(rowId).processNewInput(prediction);
                    }
            );
        }

        return intermediatePredictions.stream().sequential()
                .map(intPred -> intPred.transformToOutput())
                .collect(Collectors.toList());
    }


    @Override
    public List<FO> evaluateOOB(List<? extends CovariateRow> rowList){
        final List<IntermediateCombinedResponse<O, FO>> intermediatePredictions =
                IntStream.range(0, rowList.size())
                        .mapToObj(i -> treeResponseCombiner.startIntermediateCombinedResponse(treeFiles.length))
                        .collect(Collectors.toList());

        final Iterator<Tree<O>> treeIterator = getTrees().iterator();
        for(int treeId = 0; treeId < treeFiles.length; treeId++){
            final Tree<O> currentTree = treeIterator.next();

            IntStream.range(0, rowList.size()).parallel().forEach(
                    rowId -> {
                        final CovariateRow row = rowList.get(rowId);
                        if(!currentTree.idInBootstrapSample(row.getId())){
                            final O prediction = currentTree.evaluate(row);
                            intermediatePredictions.get(rowId).processNewInput(prediction);
                        }
                        // else do nothing; when we get the final output it will get scaled for the smaller N
                    }
            );
        }

        return intermediatePredictions.stream().parallel()
                .map(intPred -> intPred.transformToOutput())
                .collect(Collectors.toList());
    }

    @Override
    public List<FO> evaluateSerialOOB(List<? extends CovariateRow> rowList){
        final List<IntermediateCombinedResponse<O, FO>> intermediatePredictions =
                IntStream.range(0, rowList.size())
                        .mapToObj(i -> treeResponseCombiner.startIntermediateCombinedResponse(treeFiles.length))
                        .collect(Collectors.toList());

        final Iterator<Tree<O>> treeIterator = getTrees().iterator();
        for(int treeId = 0; treeId < treeFiles.length; treeId++){
            final Tree<O> currentTree = treeIterator.next();

            IntStream.range(0, rowList.size()).sequential().forEach(
                    rowId -> {
                        final CovariateRow row = rowList.get(rowId);
                        if(!currentTree.idInBootstrapSample(row.getId())){
                            final O prediction = currentTree.evaluate(row);
                            intermediatePredictions.get(rowId).processNewInput(prediction);
                        }
                        // else do nothing; when we get the final output it will get scaled for the smaller N
                    }
            );
        }

        return intermediatePredictions.stream().sequential()
                .map(intPred -> intPred.transformToOutput())
                .collect(Collectors.toList());
    }

    @Override
    public Iterable<Tree<O>> getTrees() {
        return new IterableOfflineTree<>(treeFiles);
    }

    @Override
    public int getNumberOfTrees() {
        return treeFiles.length;
    }

    public OnlineForest<O, FO> createOnlineCopy(){
        final List<Tree<O>> allTrees = new ArrayList<>(getNumberOfTrees());
        getTrees().forEach(allTrees::add);

        return OnlineForest.<O, FO>builder()
                .trees(allTrees)
                .treeResponseCombiner(treeResponseCombiner)
                .build();
    }
}

