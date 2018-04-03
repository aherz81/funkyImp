
package com.sun.source.tree;

import java.util.List;
import javax.lang.model.element.Name;

/**
 * A tree node for a arg expression
 * type declaration.
 *
 * For example:
 * <pre>
 *
 *  <em>expression</em>
 *
 * or
 *
 * [ <em>expression</em> , <em>expression</em> ]
 *   
 * </pre>
 *
 *
 * @author Alex Herz
 * @since 0.0
 */
public interface ArgExpressionTree extends ExpressionTree {
    ExpressionTree getExpression1();
    ExpressionTree getExpression2();
}
