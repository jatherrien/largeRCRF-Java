package ca.joeltherrien.randomforest.tree;

import ca.joeltherrien.randomforest.CovariateRow;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor
@ToString
public class TerminalNode<Y> implements Node<Y> {

    private final Y responseValue;

    @Override
    public Y evaluate(CovariateRow row){
        return responseValue;
    }


}
