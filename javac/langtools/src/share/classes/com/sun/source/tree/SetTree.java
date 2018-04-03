package com.sun.source.tree;
import java.util.List;
/**
 * A tree node for a set of expressions
 *
 *
 * @see "The Java Language Specification, 3rd ed, section 14.6"
 *
 * @author Peter von der Ah&eacute;
 * @author Jonathan Gibbons
 * @since 1.6
 */
public interface SetTree extends ExpressionTree
{
    List<? extends ExpressionTree> getContent();
}
