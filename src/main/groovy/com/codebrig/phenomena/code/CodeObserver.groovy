package com.codebrig.phenomena.code

import com.codebrig.omnisrc.SourceNodeFilter
import com.codebrig.omnisrc.observe.filter.WildcardFilter

import javax.validation.constraints.NotNull

/**
 * Represents an entity which observers the properties of source code nodes
 * in order to extract/calculate additional source code properties.
 *
 * @version 0.2
 * @since 0.1
 * @author <a href="mailto:brandon.fergerson@codebrig.com">Brandon Fergerson</a>
 */
trait CodeObserver {

    abstract void applyObservation(ContextualNode node, ContextualNode parentNode)

    void reset() {
    }

    @NotNull
    SourceNodeFilter getFilter() {
        return new WildcardFilter()
    }

    @NotNull
    String getSchema() {
        return ""
    }
}
