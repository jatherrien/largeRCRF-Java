package ca.joeltherrien.randomforest.tree;

import ca.joeltherrien.randomforest.CovariateRow;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@RequiredArgsConstructor
public class Tree<Y> implements Node<Y> {

    private final Node<Y> rootNode;
    private final int[] bootstrapRowIds;
    private boolean bootStrapRowIdsSorted = false;

    @Override
    public Y evaluate(CovariateRow row) {
        return rootNode.evaluate(row);
    }

    public int[] getBootstrapRowIds(){
        return bootstrapRowIds.clone();
    }

    /**
     * Sort bootstrapRowIds. This is not done automatically for efficiency purposes, as in many cases we may not be interested in using bootstrapRowIds();
     *
     */
    public void sortBootstrapRowIds(){
        if(!bootStrapRowIdsSorted){
            Arrays.sort(bootstrapRowIds);
            bootStrapRowIdsSorted = true;
        }

    }

    public boolean idInBootstrapSample(int id){
        this.sortBootstrapRowIds();

        return Arrays.binarySearch(this.bootstrapRowIds, id) >= 0;
    }

}
