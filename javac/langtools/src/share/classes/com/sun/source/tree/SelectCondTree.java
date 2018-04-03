

package com.sun.source.tree;

/**
 * A tree node for an assignment expression.
 *
 * For example:
 * <pre>
 *   | <em>cond</em> -> <em>res</em>
 * </pre>
 *
 * @author Alex Herz
 * @since 0.0
 */
public interface SelectCondTree extends ExpressionTree {
    ExpressionTree getCond();
    ExpressionTree getRes();
}
