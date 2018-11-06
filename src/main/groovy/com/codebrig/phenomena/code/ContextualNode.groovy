package com.codebrig.phenomena.code

import com.codebrig.omnisrc.SourceLanguage
import gopkg.in.bblfsh.sdk.v1.uast.generated.Node

/**
 * todo: description
 *
 * @version 0.2
 * @since 0.1
 * @author <a href="mailto:brandon.fergerson@codebrig.com">Brandon Fergerson</a>
 */
class ContextualNode {

    private final static IdentityHashMap<Node, ContextualNode> contextualNodes = new IdentityHashMap<>()

    static ContextualNode getContextualNode(SourceLanguage language, Node node) {
        contextualNodes.putIfAbsent(node, new ContextualNode(language, node))
        return contextualNodes.get(node)
    }

    private final SourceLanguage language
    private final Node underlyingNode
    private IdentityHashMap<DataKey<?>, Object> data = new IdentityHashMap<>()

    private ContextualNode(SourceLanguage language, Node underlyingNode) {
        this.language = Objects.requireNonNull(language)
        this.underlyingNode = Objects.requireNonNull(underlyingNode)
    }

    def <M> M getData(final DataKey<M> key) {
        if (data == null) {
            return null
        }
        return (M) data.get(key)
    }

    def <M> void setData(DataKey<M> key, M object) {
        if (data == null) {
            data = new IdentityHashMap<>()
        }
        data.put(key, object)
    }

    SourceLanguage getLanguage() {
        return language
    }

    Node getUnderlyingNode() {
        return underlyingNode
    }

}
