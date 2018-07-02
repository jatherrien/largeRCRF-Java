package ca.joeltherrien.randomforest.tree;

import ca.joeltherrien.randomforest.CovariateRow;

public interface Node<Y> {

    Y evaluate(CovariateRow row);

}
