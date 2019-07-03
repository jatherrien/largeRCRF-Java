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

package ca.joeltherrien.randomforest.utils;

public abstract class StepFunction implements MathFunction{

    protected final double[] x;

    StepFunction(double[] x){
        this.x = x;
    }

    public double[] getX() {
        return x.clone();
    }

    public abstract double evaluateByIndex(int i);

    /**
     * Evaluate the function at the time *point* that occurred previous to time. This is NOT time - some delta, but rather
     * time[i-1].
     *
     * @param time
     * @return
     */
    public abstract double evaluatePrevious(double time);

}
