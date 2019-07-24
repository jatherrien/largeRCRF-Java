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

import ca.joeltherrien.randomforest.CovariateRow;
import ca.joeltherrien.randomforest.Row;
import ca.joeltherrien.randomforest.covariates.Covariate;
import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskResponse;
import ca.joeltherrien.randomforest.responses.competingrisk.CompetingRiskResponseWithCensorTime;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * These static methods are designed to make the R interface more performant; and to avoid using R loops.
 *
 */
public final class RUtils {

    public static double[] extractTimes(final RightContinuousStepFunction function){
        return function.getX();
    }

    public static double[] extractY(final RightContinuousStepFunction function){
        return function.getY();
    }

    /**
     * Convenience method to help R package serialize Java objects.
     *
     * @param object The object to be serialized.
     * @param filename The path to the object to be saved.
     * @throws IOException
     */
    public static void serializeObject(Serializable object, String filename) throws IOException {
        final ObjectOutputStream outputStream = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(filename)));
        outputStream.writeObject(object);
        outputStream.close();
    }

    /**
     * Convenience method to help R package load serialized Java objects.
     *
     * @param filename The path to the object saved.
     * @throws IOException
     */
    public static Object loadObject(String filename) throws IOException, ClassNotFoundException {
        final ObjectInputStream inputStream = new ObjectInputStream(new GZIPInputStream(new FileInputStream(filename)));
        final Object object = inputStream.readObject();
        inputStream.close();

        return object;

    }

    public static <Y> List<Row<Y>> importDataWithResponses(List<Y> responses, List<Covariate> covariates, List<String[]> rawCovariateData){
        if(covariates.size() != rawCovariateData.size()){
            throw new IllegalArgumentException("covariates size doesn't match number of columns in rawCovariateData; there must be a one-to-one relationship");
        }

        final int n = responses.size();
        final int p = covariates.size();
        final List<Row<Y>> rowList = new ArrayList<>(n);

        // Let's verify the size first
        for(int j=0; j<p; j++){
            if(rawCovariateData.get(j).length != n){
                final String covariateWithBadLength = covariates.get(j).getName();
                throw new IllegalArgumentException(
                        "Length of covariate " + covariateWithBadLength +
                                "(" + rawCovariateData.get(j).length +
                                ") does not match length of responses (" + n + ").");
            }
        }

        for(int i=0; i<n; i++){

            final Covariate.Value[] valueArray = new Covariate.Value[p];
            for(int j=0; j<p; j++){
                final Covariate covariate = covariates.get(j);
                final String rawValue = rawCovariateData.get(j)[i];

                final Covariate.Value value = covariate.createValue(rawValue);
                valueArray[j] = value;
            }

            final Row<Y> newRow = new Row<>(valueArray, i+1, responses.get(i));
            rowList.add(newRow);

        }

        return rowList;
    }

    public static List<CovariateRow> importData(List<Covariate> covariates, List<String[]> rawCovariateData){
        if(covariates.size() != rawCovariateData.size()){
            throw new IllegalArgumentException("covariates size doesn't match number of columns in rawCovariateData; there must be a one-to-one relationship");
        }

        final int n = rawCovariateData.get(0).length;
        final int p = covariates.size();
        final List<CovariateRow> rowList = new ArrayList<>(n);

        for(int i=0; i<n; i++){

            final Covariate.Value[] valueArray = new Covariate.Value[p];
            for(int j=0; j<p; j++){
                final Covariate covariate = covariates.get(j);
                final String rawValue = rawCovariateData.get(j)[i];

                final Covariate.Value value = covariate.createValue(rawValue);
                valueArray[j] = value;
            }

            final CovariateRow newRow = new CovariateRow(valueArray, i+1);
            rowList.add(newRow);

        }

        return rowList;
    }

    public static List<CompetingRiskResponseWithCensorTime> importCompetingRiskResponsesWithCensorTimes(
            final int[] eventIndicators,
            final double[] eventTimes,
            final double[] censorTimes){

        final int n = eventIndicators.length;

        if(eventTimes.length != n || censorTimes.length != n){
            throw new IllegalArgumentException("Array lengths must match");
        }

        final List<CompetingRiskResponseWithCensorTime> responseList = new ArrayList<>(n);

        for(int i=0; i<n; i++){
            responseList.add(new CompetingRiskResponseWithCensorTime(eventIndicators[i], eventTimes[i], censorTimes[i]));
        }

        return responseList;

    }

    public static List<CompetingRiskResponse> importCompetingRiskResponses(
            final int[] eventIndicators,
            final double[] eventTimes){

        final int n = eventIndicators.length;

        if(eventTimes.length != n){
            throw new IllegalArgumentException("Array lengths must match");
        }

        final List<CompetingRiskResponse> responseList = new ArrayList<>(n);

        for(int i=0; i<n; i++){
            responseList.add(new CompetingRiskResponse(eventIndicators[i], eventTimes[i]));
        }

        return responseList;

    }

    public static List<Double> importNumericResponse(double[] values){
        final List<Double> responses = new ArrayList<>(values.length);

        for(double value : values){
            responses.add(value);
        }

        return responses;
    }

    public static List<Object> produceSublist(List<Object> initialList, int[] indices){
        final List<Object> newList = new ArrayList<>(indices.length);

        for(int i : indices){
            newList.add(initialList.get(i));
        }

        return newList;
    }

}
