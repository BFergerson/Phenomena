package com.codebrig.phenomena.code

import ai.grakn.Keyspace
import ai.grakn.client.Grakn
import com.codebrig.phenomena.code.structure.StructureVisitor

/**
 * todo: description
 *
 * @version 0.1
 * @since 0.1
 * @author <a href="mailto:brandon.fergerson@codebrig.com">Brandon Fergerson</a>
 */
class CodeObserverVisitor implements StructureVisitor {

    private final Keyspace keyspace
    private final List<CodeObserver> observers

    CodeObserverVisitor(Keyspace keyspace) {
        this.keyspace = Objects.requireNonNull(keyspace)
        this.observers = new ArrayList<>()
    }

    void addObserver(CodeObserver observer) {
        observers.add(Objects.requireNonNull(observer))
    }

    List<CodeObserver> getObservers() {
        return new ArrayList<>(observers)
    }

    @Override
    void visit(ContextualNode n, Grakn.Transaction innerTx) {
        Objects.requireNonNull(n)
        Objects.requireNonNull(innerTx)
        observers.each {
            it.applyObservation(n, innerTx.graql())
        }
    }

}
