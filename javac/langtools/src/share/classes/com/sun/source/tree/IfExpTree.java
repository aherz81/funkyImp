
package com.sun.source.tree;

/**
 * A tree node for an 'if' statement.
 *
 * For example:
 * <pre>
 *   if ( <em>condition</em> )
 *      {<em>thenExp</em>}
 *
 *   if ( <em>condition</em> )
 *       {<em>thenExp</em>}
 *   else
 *       {<em>elseExp</em>}
 * </pre>

 * @author aherz
 * @since 0.0
 */
public interface IfExpTree extends ExpressionTree {
    ExpressionTree getCondition();
    ExpressionTree getThenExp();
    /**
     * @return null if this if statement has no else branch.
     */
    ExpressionTree getElseExp();
}
