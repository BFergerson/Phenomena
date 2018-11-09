package com.codebrig.phenomena.code

import ai.grakn.Keyspace
import ai.grakn.client.Grakn
import ai.grakn.graql.QueryBuilder
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
class CodeObserverVisitor {

    private final Keyspace keyspace
    private final List<CodeObserver> observers
    private final IdentityHashMap<CodeObserver, ContextualNode> previousNodes
    private final IdentityHashMap<Node, ContextualNode> contextualNodes

    CodeObserverVisitor(Keyspace keyspace) {
        this.keyspace = Objects.requireNonNull(keyspace)
        this.observers = new ArrayList<>()
        this.previousNodes = new IdentityHashMap<>()
        this.contextualNodes = new IdentityHashMap<>()
    }

    void addObserver(CodeObserver observer) {
        observers.add(Objects.requireNonNull(observer))
    }

    List<CodeObserver> getObservers() {
        return new ArrayList<>(observers)
    }

    void visit(SourceLanguage language, Node rootNode, Grakn.Transaction transaction) {
        Objects.requireNonNull(rootNode)
        Objects.requireNonNull(transaction)

        contextualNodes.putIfAbsent(rootNode, new ContextualNode(this, rootNode, language, rootNode))
        def contextualRootNode = contextualNodes.get(rootNode)
        def graql = transaction.graql()
        observers.each {
            if (it.filter.evaluate(contextualRootNode)) {
                previousNodes.put(it, contextualRootNode)
                it.applyObservation(graql, contextualRootNode, null, null)
            }
        }
        visitRecursively(graql, contextualRootNode, contextualRootNode.children)
    }

    private void visitRecursively(QueryBuilder graql, ContextualNode parentNode, Iterator<SourceNode> childNodes) {
        childNodes.each { child ->
            contextualNodes.putIfAbsent(child.underlyingNode, new ContextualNode(this, child))
            def contextualChildNode = contextualNodes.get(child.underlyingNode)

            observers.each {
                if (it.filter.evaluate(contextualChildNode)) {
                    if (previousNodes.containsKey(it)) {
                        it.applyObservation(graql, contextualChildNode, parentNode, previousNodes.get(it))
                    } else {
                        it.applyObservation(graql, contextualChildNode, parentNode, null)
                    }
                    previousNodes.put(it, contextualChildNode)
                }
            }
            visitRecursively(graql, contextualChildNode, child.children)
        }
    }

    ContextualNode getContextualNode(Node node) {
        return contextualNodes.get(node)
    }
}
