package com.codebrig.phenomena.code

import ai.grakn.graql.QueryBuilder
import com.codebrig.omnisrc.SourceFilter
import com.codebrig.omnisrc.schema.filter.WildcardFilter

import javax.validation.constraints.NotNull

/**
 * todo: description
 *
 * @version 0.2
 * @since 0.1
 * @author <a href="mailto:brandon.fergerson@codebrig.com">Brandon Fergerson</a>
 */
trait CodeObserver {

    abstract void applyObservation(QueryBuilder qb, ContextualNode node,
                                   ContextualNode parentNode, ContextualNode previousNode)

    @NotNull
    SourceFilter getFilter() {
        return new WildcardFilter()
    }

    @NotNull
    String getSchema() {
        return ""
    }
}
