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
    private final Map<Integer, ContextualNode> contextualNodes
    private final boolean saveToGrakn

    CodeObserverVisitor() {
        this.saveToGrakn = false
        this.session = null
        this.observers = new ArrayList<>()
        this.contextualNodes = new ConcurrentHashMap<>()
    }

    CodeObserverVisitor(Grakn.Session session) {
        this.saveToGrakn = true
        this.session = Objects.requireNonNull(session)
        this.observers = new ArrayList<>()
        this.contextualNodes = new ConcurrentHashMap<>()
    }

    void addObserver(CodeObserver observer) {
        observers.add(Objects.requireNonNull(observer))
        observer.codeObserverVisitor = this
    }

    void addObservers(List<CodeObserver> observers) {
        Objects.requireNonNull(observers).each {
            addObserver(it)
        }
    }

    List<CodeObserver> getObservers() {
        return new ArrayList<>(observers)
    }

    ContextualNode visit(SourceLanguage language, Node rootNode, File sourceFile) {
        Objects.requireNonNull(language)
        Objects.requireNonNull(rootNode)

        def observed = false
        contextualNodes.putIfAbsent(System.identityHashCode(rootNode),
                new ContextualNode(this, rootNode, sourceFile, language, rootNode))
        def contextualRootNode = contextualNodes.get(System.identityHashCode(rootNode))
        observers.each {
            if (it.filter.evaluate(contextualRootNode)) {
                observed = true
                it.applyObservation(contextualRootNode, null)
            }
        }

        ContextualNode rootObservedNode
        def transaction = null
        def queryBuilder = null
        if (saveToGrakn) {
            transaction = session.transaction(GraknTxType.WRITE)
            queryBuilder = transaction.graql()
            if (observed) {
                contextualRootNode.save(queryBuilder)
                if (rootObservedNode == null) {
                    rootObservedNode = contextualRootNode
                }
            }
        }
        if (rootObservedNode == null) {
            rootObservedNode = visitCompletely(queryBuilder, sourceFile, contextualRootNode)
        } else {
            visitCompletely(queryBuilder, sourceFile, contextualRootNode)
        }

        if (saveToGrakn) {
            contextualNodes.forEach({ key, node ->
                node.save(queryBuilder)
                if (rootObservedNode == null) {
                    rootObservedNode = node
                }

                if (node.underlyingNode != node.rootNode) {
                    contextualNodes.remove(key)
                }
            })
        }
        observers.each {
            it.reset()
        }
        transaction?.commit()
        transaction?.close()
        return rootObservedNode
    }

    private ContextualNode visitCompletely(QueryBuilder qb, File sourceFile, ContextualNode rootSourceNode) {
        ContextualNode rootObservedNode = null
        Stack<ContextualNode> parentStack = new Stack<>()
        Stack<Iterator<SourceNode>> childrenStack = new Stack<>()
        parentStack.push(rootSourceNode)
        childrenStack.push(rootSourceNode.children)

        while (!parentStack.isEmpty() && !childrenStack.isEmpty()) {
            def parent = parentStack.pop()
            def children = childrenStack.pop()

            children.each {
                def contextualChildNode = getOrCreateContextualNode(it, sourceFile)
                def observed = false

                observers.each {
                    if (it.filter.evaluate(contextualChildNode)) {
                        observed = true
                        it.applyObservation(contextualChildNode, parent)
                    }
                }
                if (observed) {
                    if (saveToGrakn) {
                        contextualChildNode.save(qb)
                        if (rootObservedNode == null) {
                            rootObservedNode = contextualChildNode
                        }
                    }

                    parentStack.push(contextualChildNode)
                    childrenStack.push(contextualChildNode.children)
                } else {
                    parentStack.push(parent)
                    childrenStack.push(contextualChildNode.children)
                }
            }
        }
        return rootObservedNode
    }

    ContextualNode getContextualNode(Node node) {
        return contextualNodes.get(System.identityHashCode(node))
    }

    ContextualNode getOrCreateContextualNode(SourceNode node, File sourceFile) {
        def nexContextualNode
        def existingContextualNode = contextualNodes.putIfAbsent(System.identityHashCode(node.underlyingNode),
                nexContextualNode = new ContextualNode(this, node, sourceFile))
        return existingContextualNode != null ? existingContextualNode : nexContextualNode
    }

    boolean getSaveToGrakn() {
        return saveToGrakn
    }
}
