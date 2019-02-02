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
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

public class Tree<Y> implements Node<Y> {

    @Getter
    private final Node<Y> rootNode;
    private final int[] bootstrapRowIds;


    public Tree(Node<Y> rootNode, int[] bootstrapRowIds) {
        this.rootNode = rootNode;
        this.bootstrapRowIds = bootstrapRowIds;
        Arrays.sort(bootstrapRowIds);
    }

    @Override
    public Y evaluate(CovariateRow row) {
        return rootNode.evaluate(row);
    }

    @Override
    public <C extends Node<Y>> List<C> getNodesOfType(Class<C> nodeType) {
        return rootNode.getNodesOfType(nodeType);
    }

    public int[] getBootstrapRowIds(){
        return bootstrapRowIds.clone();
    }

    public boolean idInBootstrapSample(int id){
        return Arrays.binarySearch(this.bootstrapRowIds, id) >= 0;
    }

    @Override
    public String toString(){
        return rootNode.toString();
    }
}
