

package com.sun.source.tree;

/**
 * A tree node for an assignment expression.
 *
 * For example:
 * <pre>
 *   <em>variable</em> = <em>expression</em> where <em>statement</em>
 * </pre>
 *
 * @author Alex Herz
 * @since 0.0
 */
public interface WhereTree extends ExpressionTree {
    ExpressionTree getExpression();
    StatementTree getStatement();
    ExpressionTree getSExpression();
}
