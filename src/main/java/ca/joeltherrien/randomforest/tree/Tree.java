package ca.joeltherrien.randomforest.tree;

import ca.joeltherrien.randomforest.CovariateRow;

import java.util.Arrays;

public class Tree<Y> implements Node<Y> {

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

    public int[] getBootstrapRowIds(){
        return bootstrapRowIds.clone();
    }

    public boolean idInBootstrapSample(int id){
        return Arrays.binarySearch(this.bootstrapRowIds, id) >= 0;
    }

}
