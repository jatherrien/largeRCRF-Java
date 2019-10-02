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

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public abstract class Forest<O, FO> {

    public abstract FO evaluate(CovariateRow row);
    public abstract FO evaluateOOB(CovariateRow row);
    public abstract Iterable<Tree<O>> getTrees();
    public abstract int getNumberOfTrees();

    /**
     * Used primarily in the R package interface to avoid R loops; and for easier parallelization.
     *
     * @param rowList List of CovariateRows to evaluate
     * @return A List of predictions.
     */
    public List<FO> evaluate(List<? extends CovariateRow> rowList){
        return rowList.parallelStream()
                .map(this::evaluate)
                .collect(Collectors.toList());
    }

    /**
     * Used primarily in the R package interface to avoid R loops without parallelization.
     * I suspect that on some cluster systems using a parallelStream can cause serious crashes.
     *
     * @param rowList List of CovariateRows to evaluate
     * @return A List of predictions.
     */
    public List<FO> evaluateSerial(List<? extends CovariateRow> rowList){
        return rowList.stream()
                .map(this::evaluate)
                .collect(Collectors.toList());
    }

    /**
     * Used primarily in the R package interface to avoid R loops; and for easier parallelization.
     *
     * @param rowList List of CovariateRows to evaluate OOB
     * @return A List of predictions.
     */
    public List<FO> evaluateOOB(List<? extends CovariateRow> rowList){
        return rowList.parallelStream()
                .map(this::evaluateOOB)
                .collect(Collectors.toList());
    }

    /**
     * Used primarily in the R package interface to avoid R loops without parallelization.
     * I suspect that on some cluster systems using a parallelStream can cause serious crashes.
     *
     * @param rowList List of CovariateRows to evaluate OOB
     * @return A List of predictions.
     */
    public List<FO> evaluateSerialOOB(List<? extends CovariateRow> rowList){
        return rowList.stream()
                .map(this::evaluateOOB)
                .collect(Collectors.toList());
    }

    public Map<Integer, Integer> findSplitsByCovariate(){
        final Map<Integer, Integer> countMap = new TreeMap<>();

        for(final Tree<O> tree : getTrees()){
            final Node<O> rootNode = tree.getRootNode();
            final List<SplitNode> splitNodeList = rootNode.getNodesOfType(SplitNode.class);

            for(final SplitNode splitNode : splitNodeList){
                final Integer covariateIndex = splitNode.getSplitRule().getParentCovariateIndex();

                final Integer currentCount = countMap.getOrDefault(covariateIndex, 0);
                countMap.put(covariateIndex, currentCount+1);
            }

        }

        return countMap;
    }

    public double averageTerminalNodeSize(){
        long numberTerminalNodes = 0;
        long totalSizeTerminalNodes = 0;

        for(final Tree<O> tree : getTrees()){
            final Node<O> rootNode = tree.getRootNode();
            final List<TerminalNode> terminalNodeList = rootNode.getNodesOfType(TerminalNode.class);

            for(final TerminalNode terminalNode : terminalNodeList){
                numberTerminalNodes++;
                totalSizeTerminalNodes += terminalNode.getSize();
            }

        }

        return (double) totalSizeTerminalNodes / (double) numberTerminalNodes;
    }

    public int numberOfTerminalNodes(){
        int countTerminalNodes = 0;

        for(final Tree<O> tree : getTrees()){
            final Node<O> rootNode = tree.getRootNode();
            final List<TerminalNode> terminalNodeList = rootNode.getNodesOfType(TerminalNode.class);

            countTerminalNodes += terminalNodeList.size();
        }

        return countTerminalNodes;
    }


}
