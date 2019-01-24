package ca.joeltherrien.randomforest.covariates;

import ca.joeltherrien.randomforest.Row;
import ca.joeltherrien.randomforest.covariates.numeric.NumericCovariate;
import ca.joeltherrien.randomforest.covariates.numeric.NumericSplitRuleUpdater;
import ca.joeltherrien.randomforest.covariates.numeric.NumericSplitUpdate;
import ca.joeltherrien.randomforest.tree.Split;
import ca.joeltherrien.randomforest.utils.IndexedIterator;
import ca.joeltherrien.randomforest.utils.UniqueSubsetValueIterator;
import ca.joeltherrien.randomforest.utils.UniqueValueIterator;
import ca.joeltherrien.randomforest.utils.Utils;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class NumericCovariateTest {

    private List<Row<Double>> createTestDataset(NumericCovariate covariate){
        final List<Row<Double>> rowList = new ArrayList<>();
        final List<Covariate> covariateList = Collections.singletonList(covariate);

        rowList.add(Row.createSimple(Utils.easyMap("x", "1.0"), covariateList, 1, 1.0));
        rowList.add(Row.createSimple(Utils.easyMap("x", "2.0"), covariateList, 2, 1.0));
        rowList.add(Row.createSimple(Utils.easyMap("x", "2.0"), covariateList, 3, 1.0));
        rowList.add(Row.createSimple(Utils.easyMap("x", "3.0"), covariateList, 4, 1.0));
        rowList.add(Row.createSimple(Utils.easyMap("x", "3.0"), covariateList, 5, 1.0));
        rowList.add(Row.createSimple(Utils.easyMap("x", "4.0"), covariateList, 6, 1.0));
        rowList.add(Row.createSimple(Utils.easyMap("x", "4.0"), covariateList, 7, 1.0));

        return rowList;
    }

    @Test
    public void testNumericCovariateDeterministic(){
        final NumericCovariate covariate = new NumericCovariate("x", 0);

        final List<Row<Double>> dataset = createTestDataset(covariate);

        // nsplit=0 so result should be deterministic
        final NumericSplitRuleUpdater<Double> splitRuleUpdater = covariate.generateSplitRuleUpdater(dataset, 0, new Random());

        // Current Split should be empty on the left side
        Split<Double, Double> currentSplit = splitRuleUpdater.currentSplit();
        assertEquals(0, currentSplit.getLeftHand().size());
        assertEquals(dataset.size(), currentSplit.getRightHand().size());
        assertFalse(currentSplit.getSplitRule().isLeftHand(covariate.createValue(-1.0))); // everything is right hand; we have no negative values

        /* First Update */
        assertTrue(splitRuleUpdater.hasNext());
        NumericSplitUpdate<Double> update = splitRuleUpdater.nextUpdate();
        assertEquals(1, update.rowsMovedToLeftHand().size());
        assertEquals(dataset.get(0), update.rowsMovedToLeftHand().get(0));

        int index = 1;
        currentSplit = splitRuleUpdater.currentSplit();
        assertEquals(index, currentSplit.getLeftHand().size());
        assertContains(dataset.subList(0,index), currentSplit.getLeftHand());
        assertEquals(dataset.size()-index, currentSplit.getRightHand().size());
        assertContains(dataset.subList(index, dataset.size()), currentSplit.getRightHand());
        assertTrue(currentSplit.getSplitRule().isLeftHand(covariate.createValue(1.0)));
        assertFalse(currentSplit.getSplitRule().isLeftHand(covariate.createValue(1.5)));


        /* Second Update */
        assertTrue(splitRuleUpdater.hasNext());
        update = splitRuleUpdater.nextUpdate();
        assertEquals(2, update.rowsMovedToLeftHand().size());
        assertTrue(update.rowsMovedToLeftHand().contains(dataset.get(1)));
        assertTrue(update.rowsMovedToLeftHand().contains(dataset.get(2)));

        index = 3;
        currentSplit = splitRuleUpdater.currentSplit();
        assertEquals(index, currentSplit.getLeftHand().size());
        assertContains(dataset.subList(0,index), currentSplit.getLeftHand());
        assertEquals(dataset.size()-index, currentSplit.getRightHand().size());
        assertContains(dataset.subList(index, dataset.size()), currentSplit.getRightHand());
        assertTrue(currentSplit.getSplitRule().isLeftHand(covariate.createValue(2.0)));
        assertFalse(currentSplit.getSplitRule().isLeftHand(covariate.createValue(2.5)));



        /* Third Update */
        assertTrue(splitRuleUpdater.hasNext());
        update = splitRuleUpdater.nextUpdate();
        assertEquals(2, update.rowsMovedToLeftHand().size());
        assertTrue(update.rowsMovedToLeftHand().contains(dataset.get(3)));
        assertTrue(update.rowsMovedToLeftHand().contains(dataset.get(4)));

        index = 5;
        currentSplit = splitRuleUpdater.currentSplit();
        assertEquals(index, currentSplit.getLeftHand().size());
        assertContains(dataset.subList(0,index), currentSplit.getLeftHand());
        assertEquals(dataset.size()-index, currentSplit.getRightHand().size());
        assertContains(dataset.subList(index, dataset.size()), currentSplit.getRightHand());
        assertTrue(currentSplit.getSplitRule().isLeftHand(covariate.createValue(3.0)));
        assertFalse(currentSplit.getSplitRule().isLeftHand(covariate.createValue(3.5)));



        /* Fourth Update */
        assertTrue(splitRuleUpdater.hasNext());
        update = splitRuleUpdater.nextUpdate();
        assertEquals(2, update.rowsMovedToLeftHand().size());
        assertTrue(update.rowsMovedToLeftHand().contains(dataset.get(5)));
        assertTrue(update.rowsMovedToLeftHand().contains(dataset.get(6)));

        index = 7;
        currentSplit = splitRuleUpdater.currentSplit();
        assertEquals(index, currentSplit.getLeftHand().size());
        assertContains(dataset.subList(0,index), currentSplit.getLeftHand());
        assertEquals(dataset.size()-index, currentSplit.getRightHand().size());
        assertContains(dataset.subList(index, dataset.size()), currentSplit.getRightHand());
        assertTrue(currentSplit.getSplitRule().isLeftHand(covariate.createValue(4.0)));
        assertFalse(currentSplit.getSplitRule().isLeftHand(covariate.createValue(4.5)));


        assertFalse(splitRuleUpdater.hasNext());

    }

    @Test
    public void testNumericSplitRuleUpdaterWithIndexes(){
        final NumericCovariate covariate = new NumericCovariate("x", 0);

        final List<Row<Double>> dataset = createTestDataset(covariate);

        final Integer[] indexes = new Integer[]{2,3,4};

        final IndexedIterator<Double> dataIterator = new UniqueSubsetValueIterator<>(
                new UniqueValueIterator<>(dataset.stream()
                        .map(row -> row.getCovariateValue(covariate).getValue()).iterator()),
                indexes
        );

        final NumericSplitRuleUpdater<Double> splitRuleUpdater = new NumericSplitRuleUpdater<>(covariate, dataset, dataIterator);

        // Current Split should be empty on the left side
        Split<Double, Double> currentSplit = splitRuleUpdater.currentSplit();
        assertEquals(0, currentSplit.getLeftHand().size());
        assertEquals(dataset.size(), currentSplit.getRightHand().size());
        assertFalse(currentSplit.getSplitRule().isLeftHand(covariate.createValue(-1.0))); // everything is right hand; we have no negative values

        /* First Update */
        assertTrue(splitRuleUpdater.hasNext());
        NumericSplitUpdate<Double> update = splitRuleUpdater.nextUpdate();
        assertEquals(3, update.rowsMovedToLeftHand().size());
        assertTrue(update.rowsMovedToLeftHand().contains(dataset.get(0)));
        assertTrue(update.rowsMovedToLeftHand().contains(dataset.get(1)));
        assertTrue(update.rowsMovedToLeftHand().contains(dataset.get(2)));

        int index = 3;
        currentSplit = splitRuleUpdater.currentSplit();
        assertEquals(index, currentSplit.getLeftHand().size());
        assertContains(dataset.subList(0,index), currentSplit.getLeftHand());
        assertEquals(dataset.size()-index, currentSplit.getRightHand().size());
        assertContains(dataset.subList(index, dataset.size()), currentSplit.getRightHand());
        assertTrue(currentSplit.getSplitRule().isLeftHand(covariate.createValue(2.0)));
        assertFalse(currentSplit.getSplitRule().isLeftHand(covariate.createValue(2.5)));


        /* Second Update */
        assertTrue(splitRuleUpdater.hasNext());
        update = splitRuleUpdater.nextUpdate();
        assertEquals(2, update.rowsMovedToLeftHand().size());
        assertTrue(update.rowsMovedToLeftHand().contains(dataset.get(3)));
        assertTrue(update.rowsMovedToLeftHand().contains(dataset.get(4)));

        index = 5;
        currentSplit = splitRuleUpdater.currentSplit();
        assertEquals(index, currentSplit.getLeftHand().size());
        assertContains(dataset.subList(0,index), currentSplit.getLeftHand());
        assertEquals(dataset.size()-index, currentSplit.getRightHand().size());
        assertContains(dataset.subList(index, dataset.size()), currentSplit.getRightHand());
        assertTrue(currentSplit.getSplitRule().isLeftHand(covariate.createValue(3.0)));
        assertFalse(currentSplit.getSplitRule().isLeftHand(covariate.createValue(3.5)));


        assertFalse(splitRuleUpdater.hasNext());

    }

    private <T> void assertContains(List<T> subList, List<T> greaterList){
        boolean allContained = true;

        for(T candidate : subList){
            if(!greaterList.contains(candidate)){
                allContained = false;
                break;
            }
        }

        assertTrue(allContained);
    }

}
