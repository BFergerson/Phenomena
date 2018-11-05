package com.codebrig.phenomena.code

import ai.grakn.graql.QueryBuilder

import javax.validation.constraints.NotNull

/**
 * todo: description
 *
 * @version 0.1
 * @since 0.1
 * @author <a href="mailto:brandon.fergerson@codebrig.com">Brandon Fergerson</a>
 */
trait CodeObserver {

    void applyObservation(ContextualNode n, QueryBuilder qb) {
    }

    @NotNull
    String getSchema() {
        return ""
    }

}
