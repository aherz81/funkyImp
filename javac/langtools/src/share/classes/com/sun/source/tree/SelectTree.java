

package com.sun.source.tree;

import java.util.List;

/**
 * A tree node for an assignment expression.
 *
 * For example:
 * <pre>
 *   <em>select</em> { <em>expression</em> -> <em>expression</em> (| ...)}
 * </pre>
 *
 * @author Alex Herz
 * @since 0.0
 */
public interface SelectTree extends ExpressionTree {
    List<? extends Tree> getCondList();
}
