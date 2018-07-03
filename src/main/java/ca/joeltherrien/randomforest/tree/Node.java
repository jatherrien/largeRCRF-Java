package ca.joeltherrien.randomforest.tree;

import ca.joeltherrien.randomforest.CovariateRow;

import java.io.Serializable;

public interface Node<Y> extends Serializable {

    Y evaluate(CovariateRow row);

}
