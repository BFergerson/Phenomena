package com.codebrig.phenomena.code

import com.codebrig.arthur.observe.structure.StructureFilter
import com.codebrig.arthur.observe.structure.filter.WildcardFilter

/**
 * Represents an entity which observers the properties of source code nodes
 * in order to extract/calculate additional source code properties.
 *
 * @version 0.2.2
 * @since 0.1
 * @author <a href="mailto:brandon.fergerson@codebrig.com">Brandon Fergerson</a>
 */
abstract class CodeObserver {

    abstract void applyObservation(ContextualNode node, ContextualNode parentNode)

    private CodeObserverVisitor codeObserverVisitor

    void setCodeObserverVisitor(CodeObserverVisitor codeObserverVisitor) {
        this.codeObserverVisitor = codeObserverVisitor
    }

    CodeObserverVisitor getCodeObserverVisitor() {
        return codeObserverVisitor
    }

    void reset() {
    }

    StructureFilter getFilter() {
        return new WildcardFilter()
    }

    String getSchema() {
        return ""
    }

    String[] getRules() {
        return new String[0]
    }
}
