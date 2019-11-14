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
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
@ToString
public class TerminalNode<Y> implements Node<Y> {
    private static final long serialVersionUID = 1L;

    private final Y responseValue;

    @Getter
    private final int size;

    @Override
    public Y evaluate(CovariateRow row){
        return responseValue;
    }

    @Override
    public <C extends Node<Y>> List<C> getNodesOfType(Class<C> nodeType) {

        if(nodeType.isInstance(this)){
            return Collections.singletonList((C) this);
        }

        return Collections.emptyList();
    }


}
