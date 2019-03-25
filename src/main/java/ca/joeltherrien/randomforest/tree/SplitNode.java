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
import ca.joeltherrien.randomforest.covariates.SplitRule;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Builder
@ToString
@Getter
public class SplitNode<Y> implements Node<Y> {

    private final Node<Y> leftHand;
    private final Node<Y> rightHand;
    private final SplitRule splitRule;
    private final double probabilityNaLeftHand; // used when assigning NA values

    @Override
    public Y evaluate(CovariateRow row) {

        if(splitRule.isLeftHand(row, probabilityNaLeftHand)){
            return leftHand.evaluate(row);
        }
        else{
            return rightHand.evaluate(row);
        }

    }

    @Override
    public <C extends Node<Y>> List<C> getNodesOfType(Class<C> nodeType) {
        final List<C> nodeList = new ArrayList<>();
        if(nodeType.isInstance(this)){
            nodeList.add((C) this);
        }

        nodeList.addAll(leftHand.getNodesOfType(nodeType));
        nodeList.addAll(rightHand.getNodesOfType(nodeType));

        return nodeList;
    }

}
