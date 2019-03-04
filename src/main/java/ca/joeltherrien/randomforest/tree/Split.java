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

import ca.joeltherrien.randomforest.Row;
import ca.joeltherrien.randomforest.covariates.Covariate;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Very simple class that contains three lists and a SplitRule.
 * 
 * @author joel
 *
 */
@Data
public final class Split<Y, V> {

	public final Covariate.SplitRule<V> splitRule;
	public final List<Row<Y>> leftHand;
	public final List<Row<Y>> rightHand;
	public final List<Row<Y>> naHand;

	public Split<Y, V> modifiableClone(){
		return new Split<>(splitRule,
				new ArrayList<>(leftHand),
				new ArrayList<>(rightHand),
				new ArrayList<>(naHand));
	}

}
