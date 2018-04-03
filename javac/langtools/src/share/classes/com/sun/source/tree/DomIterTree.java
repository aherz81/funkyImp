package com.sun.source.tree;

import java.util.List;
import javax.lang.model.element.Name;

/**
 * A tree node for a Domain Iteration.
 *
 * For example:
 * <pre>
 *   .\<em>simpleName</em> <em>(Args)</em> { expression }
 * </pre>
 *
 * @see "FunkyImp Domain Decl"
 *
 * @author Alex Herz;
 */
public interface DomIterTree extends ExpressionTree {
    //Name getName();
    List<? extends VariableTree> getDomArgs();
    ExpressionTree getExpression();
    ExpressionTree getBody();
    StatementTree getSBody();
}
