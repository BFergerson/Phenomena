package com.codebrig.phenomena.code

/**
 * Used as an identity key for storing and
 * retrieving custom transient data to a ContextualNode
 *
 * @version 0.2
 * @since 0.1
 * @author <a href="mailto:brandon.fergerson@codebrig.com">Brandon Fergerson</a>
 */
abstract class DataKey<T> {

    @Override
    int hashCode() {
        return getClass().hashCode()
    }

    @Override
    boolean equals(Object obj) {
        return obj != null && getClass() == obj.getClass()
    }
}
