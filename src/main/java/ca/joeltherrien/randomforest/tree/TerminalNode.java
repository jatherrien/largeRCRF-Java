package ca.joeltherrien.randomforest.tree;

import ca.joeltherrien.randomforest.CovariateRow;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TerminalNode<Y> implements Node<Y> {

    private final Y responseValue;

    @Override
    public Y evaluate(CovariateRow row){
        return responseValue;
    }




}
