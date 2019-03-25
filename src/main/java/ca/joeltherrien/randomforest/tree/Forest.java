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
import ca.joeltherrien.randomforest.covariates.Covariate;
import lombok.Builder;

import java.util.*;
import java.util.stream.Collectors;

@Builder
public class Forest<O, FO> { // O = output of trees, FO = forest output. In practice O == FO, even in competing risk & survival settings

    private final Collection<Tree<O>> trees;
    private final ResponseCombiner<O, FO> treeResponseCombiner;
    private final List<Covariate> covariateList;

    public FO evaluate(CovariateRow row){

        return treeResponseCombiner.combine(
                trees.stream()
                .map(node -> node.evaluate(row))
                .collect(Collectors.toList())
        );

    }

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

    public FO evaluateOOB(CovariateRow row){

        return treeResponseCombiner.combine(
          trees.stream()
          .filter(tree -> !tree.idInBootstrapSample(row.getId()))
          .map(node -> node.evaluate(row))
          .collect(Collectors.toList())
        );

    }

    public Collection<Tree<O>> getTrees(){
        return Collections.unmodifiableCollection(trees);
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
