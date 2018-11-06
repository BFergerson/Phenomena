package com.codebrig.phenomena.code.structure

import ai.grakn.client.Grakn
import com.codebrig.phenomena.code.ContextualNode

/**
 * todo: description
 *
 * @version 0.2
 * @since 0.1
 * @author <a href="mailto:brandon.fergerson@codebrig.com">Brandon Fergerson</a>
 */
interface StructureVisitor {
    void visit(ContextualNode node, Grakn.Transaction transaction)
}
