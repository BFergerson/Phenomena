package com.codebrig.phenomena.code

import com.codebrig.arthur.SourceLanguage
import com.codebrig.arthur.SourceNode
import gopkg.in.bblfsh.sdk.v1.uast.generated.Node
import grakn.client.Grakn
import grakn.client.GraknClient
import groovy.util.logging.Slf4j

import java.util.concurrent.ConcurrentHashMap

import static java.util.Objects.requireNonNull

/**
 * Used to execute source code observers over source code files
 *
 * @version 0.2.4
 * @since 0.1
 * @author <a href="mailto:brandon.fergerson@codebrig.com">Brandon Fergerson</a>
 */
@Slf4j
class CodeObserverVisitor {

    final Grakn.Session dataSession
    private final List<CodeObserver> observers
    private final Map<Integer, ContextualNode> contextualNodes
    private final boolean saveToGrakn

    CodeObserverVisitor() {
        this.saveToGrakn = false
        this.dataSession = null
        this.observers = new ArrayList<>()
        this.contextualNodes = new ConcurrentHashMap<>()
    }

    CodeObserverVisitor(GraknClient.Core graknClient, String keyspace) {
        this.saveToGrakn = true
        this.dataSession = requireNonNull(graknClient).session(requireNonNull(keyspace), Grakn.Session.Type.DATA)
        this.observers = new ArrayList<>()
        this.contextualNodes = new ConcurrentHashMap<>()
    }

    void addObserver(CodeObserver observer) {
        observers.add(requireNonNull(observer))
        observer.codeObserverVisitor = this
    }

    void addObservers(List<CodeObserver> observers) {
        requireNonNull(observers).each {
            addObserver(it)
        }
    }

    List<CodeObserver> getObservers() {
        return new ArrayList<>(observers)
    }

    ContextualNode visit(SourceLanguage language, Node rootNode, File sourceFile) {
        requireNonNull(language)
        requireNonNull(rootNode)

        def observed = false
        ContextualNode contextualRootNode = new ContextualNode(this, rootNode, sourceFile, language, rootNode)
        observers.each {
            if (it.filter.evaluate(contextualRootNode)) {
                if (!observed) {
                    contextualNodes.putIfAbsent(System.identityHashCode(rootNode), contextualRootNode)
                }

                observed = true
                it.applyObservation(contextualRootNode, null)
            }
        }

        ContextualNode rootObservedNode
        def transaction = null
        if (saveToGrakn) {
            transaction = dataSession.transaction(Grakn.Transaction.Type.WRITE)
            if (observed) {
                contextualRootNode.save(transaction)
                if (rootObservedNode == null) {
                    rootObservedNode = contextualRootNode
                }
            }
        }
        if (rootObservedNode == null) {
            rootObservedNode = visitCompletely(transaction, sourceFile, contextualRootNode)
        } else {
            visitCompletely(transaction, sourceFile, contextualRootNode)
        }

        transaction?.commit()
        transaction?.close()
        return rootObservedNode
    }

    private ContextualNode visitCompletely(Grakn.Transaction qb, File sourceFile, ContextualNode rootSourceNode) {
        ContextualNode rootObservedNode = null
        Stack<ContextualNode> parentStack = new Stack<>()
        Stack<Iterator<SourceNode>> childrenStack = new Stack<>()
        parentStack.push(rootSourceNode)
        childrenStack.push(rootSourceNode.children)

        while (!parentStack.isEmpty() && !childrenStack.isEmpty()) {
            ContextualNode parent = parentStack.pop()
            Iterator<SourceNode> children = childrenStack.pop()

            children.each {
                if (it.internalType.isEmpty()) {
                    //https://github.com/CodeBrig/Phenomena/issues/27
                    log.warn "Skipped visiting node with missing internal type (issue codebrig/phenomena#27)"
                } else {
                    ContextualNode contextualChildNode = new ContextualNode(this, it, parent, sourceFile)
                    def observed = false

                    observers.each {
                        if (it.filter.evaluate(contextualChildNode)) {
                            if (!observed) {
                                contextualNodes.putIfAbsent(System.identityHashCode(contextualChildNode.underlyingNode),
                                        contextualChildNode)
                            }

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
        }
        return rootObservedNode
    }

    ContextualNode getContextualNode(Node node) {
        return contextualNodes.get(System.identityHashCode(node))
    }

    ContextualNode getContextualNode(SourceNode node) {
        return contextualNodes.get(System.identityHashCode(node.underlyingNode))
    }

    ContextualNode getOrCreateContextualNode(SourceNode node, File sourceFile) {
        def existingContextualNode = getContextualNode(node)
        if (existingContextualNode != null) {
            return existingContextualNode
        }

        SourceNode nodeParent = null
        for (observer in observers) {
            def validParent = observer.filter.getFilteredNodes(node, false)
            if (validParent.hasNext()) {
                nodeParent = validParent.next()
                break
            }
        }

        def nexContextualNode
        existingContextualNode = contextualNodes.putIfAbsent(System.identityHashCode(node.underlyingNode),
                nexContextualNode = new ContextualNode(this, node, nodeParent, sourceFile))
        return existingContextualNode != null ? existingContextualNode : nexContextualNode
    }

    boolean getSaveToGrakn() {
        return saveToGrakn
    }

    Collection<ContextualNode> getObservedContextualNodes() {
        return contextualNodes.values()
    }
}
