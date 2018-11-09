package com.codebrig.phenomena.code

import com.codebrig.omnisrc.SourceLanguage
import com.codebrig.omnisrc.SourceNode
import gopkg.in.bblfsh.sdk.v1.uast.generated.Node

/**
 * todo: description
 *
 * @version 0.2
 * @since 0.1
 * @author <a href="mailto:brandon.fergerson@codebrig.com">Brandon Fergerson</a>
 */
class ContextualNode extends SourceNode {

    private final CodeObserverVisitor context
    private IdentityHashMap<DataKey<?>, Object> data = new IdentityHashMap<>()

    ContextualNode(CodeObserverVisitor context, Node rootNode, SourceLanguage language, Node node) {
        super(language, rootNode, node)
        this.context = Objects.requireNonNull(context)
    }

    ContextualNode(CodeObserverVisitor context, SourceNode sourceNode) {
        super(sourceNode.language, sourceNode.rootNode, sourceNode.underlyingNode)
        this.context = Objects.requireNonNull(context)
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
}
