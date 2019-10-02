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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Builder
public class OnlineForest<O, FO> extends Forest<O, FO> { // O = output of trees, FO = forest output. In practice O == FO, even in competing risk & survival settings

    private final List<Tree<O>> trees;
    private final ResponseCombiner<O, FO> treeResponseCombiner;

    @Override
    public FO evaluate(CovariateRow row){

        return treeResponseCombiner.combine(
                trees.stream()
                .map(node -> node.evaluate(row))
                .collect(Collectors.toList())
        );

    }

    @Override
    public FO evaluateOOB(CovariateRow row){

        return treeResponseCombiner.combine(
          trees.stream()
          .filter(tree -> !tree.idInBootstrapSample(row.getId()))
          .map(node -> node.evaluate(row))
          .collect(Collectors.toList())
        );

    }

    @Override
    public List<Tree<O>> getTrees(){
        return Collections.unmodifiableList(trees);
    }

    @Override
    public int getNumberOfTrees() {
        return trees.size();
    }


}
