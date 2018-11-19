package com.codebrig.phenomena.code

import com.codebrig.omnisrc.SourceNodeFilter
import com.codebrig.omnisrc.observe.filter.WildcardFilter

import javax.validation.constraints.NotNull

/**
 * todo: description
 *
 * @version 0.2
 * @since 0.1
 * @author <a href="mailto:brandon.fergerson@codebrig.com">Brandon Fergerson</a>
 */
trait CodeObserver {

    abstract void applyObservation(ContextualNode node, ContextualNode parentNode, ContextualNode previousNode)

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
