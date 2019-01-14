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
import lombok.Builder;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Builder
public class Forest<O, FO> { // O = output of trees, FO = forest output. In practice O == FO, even in competing risk & survival settings

    private final Collection<Tree<O>> trees;
    private final ResponseCombiner<O, FO> treeResponseCombiner;

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

}
