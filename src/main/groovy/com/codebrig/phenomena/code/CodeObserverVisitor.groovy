package com.codebrig.phenomena.code

import ai.grakn.GraknTxType
import ai.grakn.client.Grakn
import ai.grakn.graql.QueryBuilder
import com.codebrig.omnisrc.SourceLanguage
import com.codebrig.omnisrc.SourceNode
import gopkg.in.bblfsh.sdk.v1.uast.generated.Node

import java.util.concurrent.ConcurrentHashMap

/**
 * Used to execute source code observers over source code files
 *
 * @version 0.2
 * @since 0.1
 * @author <a href="mailto:brandon.fergerson@codebrig.com">Brandon Fergerson</a>
 */
class CodeObserverVisitor {

    private final Grakn.Session session
    private final List<CodeObserver> observers
    private final Map<Integer, ContextualNode> previousNodes
    private final Map<Integer, ContextualNode> contextualNodes
    private final boolean saveToGrakn

    CodeObserverVisitor() {
        this.saveToGrakn = false
        this.session = null
        this.observers = new ArrayList<>()
        this.previousNodes = new ConcurrentHashMap<>()
        this.contextualNodes = new ConcurrentHashMap<>()
    }

    CodeObserverVisitor(Grakn.Session session) {
        this.saveToGrakn = true
        this.session = Objects.requireNonNull(session)
        this.observers = new ArrayList<>()
        this.previousNodes = new ConcurrentHashMap<>()
        this.contextualNodes = new ConcurrentHashMap<>()
    }

    void addObserver(CodeObserver observer) {
        observers.add(Objects.requireNonNull(observer))
    }

    List<CodeObserver> getObservers() {
        return new ArrayList<>(observers)
    }

    void visit(SourceLanguage language, Node rootNode, File sourceFile) {
        Objects.requireNonNull(language)
        Objects.requireNonNull(rootNode)
        contextualNodes.putIfAbsent(System.identityHashCode(rootNode), new ContextualNode(this, rootNode, sourceFile, language, rootNode))

        def observed = false
        def contextualRootNode = contextualNodes.get(System.identityHashCode(rootNode))
        observers.each {
            if (it.filter.evaluate(contextualRootNode)) {
                observed = true
                previousNodes.put(System.identityHashCode(it), contextualRootNode)
                it.applyObservation(contextualRootNode, null, null)
            }
        }

        def transaction = null
        def queryBuilder = null
        if (saveToGrakn) {
            transaction = session.transaction(GraknTxType.WRITE)
            queryBuilder = transaction.graql()
            if (observed) {
                contextualRootNode.save(queryBuilder)
            }
        }
        visitCompletely(queryBuilder, sourceFile, contextualRootNode)

        previousNodes.clear()
        if (saveToGrakn) {
            contextualNodes.values().each {
                it.save(queryBuilder)
            }
        }
        observers.each {
            it.reset()
        }
        transaction?.commit()
        transaction?.close()
    }

    private void visitCompletely(QueryBuilder qb, File sourceFile, ContextualNode rootSourceNode) {
        Stack<ContextualNode> parentStack = new Stack<>()
        Stack<Iterator<SourceNode>> childrenStack = new Stack<>()
        parentStack.push(rootSourceNode)
        childrenStack.push(rootSourceNode.children)

        while (!parentStack.isEmpty() && !childrenStack.isEmpty()) {
            def parent = parentStack.pop()
            def children = childrenStack.pop()

            children.each {
                contextualNodes.putIfAbsent(System.identityHashCode(it.underlyingNode), new ContextualNode(this, it, sourceFile))
                def contextualChildNode = contextualNodes.get(System.identityHashCode(it.underlyingNode))

                def observed = false
                observers.each {
                    if (it.filter.evaluate(contextualChildNode)) {
                        observed = true
                        if (previousNodes.containsKey(System.identityHashCode(it))) {
                            it.applyObservation(contextualChildNode, parent, previousNodes.get(System.identityHashCode(it)))
                        } else {
                            it.applyObservation(contextualChildNode, parent, null)
                        }
                        previousNodes.put(System.identityHashCode(it), contextualChildNode)
                    }
                }
                if (observed && saveToGrakn) {
                    contextualChildNode.save(qb)
                }

                parentStack.push(contextualChildNode)
                childrenStack.push(contextualChildNode.children)
            }
        }
    }

    ContextualNode getContextualNode(Node node) {
        return contextualNodes.get(System.identityHashCode(node))
    }

    boolean getSaveToGrakn() {
        return saveToGrakn
    }
}
