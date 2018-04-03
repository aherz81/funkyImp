package com.sun.source.tree;

import java.util.List;
import javax.lang.model.element.Name;

/**
 * A tree node for a class, interface, enum, or annotation
 * type declaration.
 *
 * For example:
 * <pre>
 *   \D <em>simpleName</em> <em>typeParameters</em>
 *   {
 *       <em>def | constraints</em>
 *   }
 * </pre>
 *
 * @see "FunkyImp Domain Decl"
 *
 * @author Alex Herz;
 */
public interface DomainTree extends StatementTree {
    Name getSimpleName();
    List<? extends DomParameterTree> getDomParameters();
    List<? extends DomParameterTree> getDomArgs();
    List<? extends Tree> getConstraints();
    List<? extends Tree> getDefs();
}
