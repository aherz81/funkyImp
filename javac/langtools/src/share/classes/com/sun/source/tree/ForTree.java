

package com.sun.source.tree;

import java.util.List;
import javax.lang.model.element.Name;
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
public interface ForTree extends StatementTree {
    Name getIdent();
    ExpressionTree getExpression();
    List<? extends Tree> getContent();
}
