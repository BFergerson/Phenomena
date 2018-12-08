package com.codebrig.phenomena.code

import ai.grakn.concept.ConceptId
import ai.grakn.graql.QueryBuilder
import ai.grakn.graql.VarPattern
import com.codebrig.omnisrc.SourceLanguage
import com.codebrig.omnisrc.SourceNode
import com.codebrig.omnisrc.observe.ObservedLanguage
import com.codebrig.phenomena.code.structure.CodeStructureObserver
import com.google.common.collect.Sets
import gopkg.in.bblfsh.sdk.v1.uast.generated.Node
import groovy.transform.Canonical

import java.util.concurrent.ConcurrentHashMap

import static ai.grakn.graql.Graql.var

/**
 * Represents a source code node (AST node) which
 * has additional contextual data associated to it.
 *
 * @version 0.2
 * @since 0.1
 * @author <a href="mailto:brandon.fergerson@codebrig.com">Brandon Fergerson</a>
 */
class ContextualNode extends SourceNode {

    private final CodeObserverVisitor context
    private final Map<DataKey<?>, Object> data = new IdentityHashMap<>()
    private final Map<String, Object> attributes = new ConcurrentHashMap<>()
    private final Map<NodeRelationship, ContextualNode> relationships = new ConcurrentHashMap<>()
    private final Set<String> roles = Sets.newConcurrentHashSet()
    private File sourceFile

    ContextualNode(CodeObserverVisitor context, Node rootNode, File sourceFile, SourceLanguage language, Node node) {
        super(language, rootNode, node)
        this.context = Objects.requireNonNull(context)
        this.sourceFile = sourceFile
    }

    ContextualNode(CodeObserverVisitor context, SourceNode sourceNode, File sourceFile) {
        super(sourceNode.language, sourceNode.rootNode, sourceNode.underlyingNode)
        this.context = Objects.requireNonNull(context)
        this.sourceFile = sourceFile
    }

    File getSourceFile() {
        return sourceFile
    }

    String getEntityType() {
        if (underlyingNode.internalType().isEmpty()) {
            return underlyingNode.internalType() //todo: understand this
        }
        return language.qualifiedName + ObservedLanguage.toValidEntity(underlyingNode.internalType())
    }

    Map<String, Object> getAttributes() {
        return attributes
    }

    Map<NodeRelationship, ContextualNode> getRelationships() {
        return relationships
    }

    void hasAttribute(String key, Object value) {
        ObservedLanguage.getLiteralAttributes()
        attributes.put(key, value)
    }

    void playsRole(String role) {
        roles.add(Objects.requireNonNull(role))
    }

    void addRelationshipTo(ContextualNode otherNode, String relationshipType) {
        addRelationshipTo(otherNode, relationshipType, "is_$relationshipType", "has_$relationshipType")
    }

    void addRelationshipTo(ContextualNode otherNode, String relationshipType, String rel1, String rel2) {
        Objects.requireNonNull(otherNode)
        Objects.requireNonNull(relationshipType)
        relationships.put(new NodeRelationship(relationshipType, rel1, rel2), otherNode)
    }

    void save(QueryBuilder qb) {
        def selfId = getData(CodeStructureObserver.SELF_ID)
        def patterns = new ArrayList<VarPattern>()
        def nodePattern = var("self").isa(entityType)
        if (selfId != null) {
            nodePattern = nodePattern.id(ConceptId.of(selfId))
        }

        boolean hasAttributes = false
        attributes.each {
            hasAttributes = true
            nodePattern = nodePattern.has(it.key, it.value)
        }
        patterns.add(nodePattern)
        boolean hasRoles = false
        roles.each {
            hasRoles = true
            patterns.add(var().isa(it)
                    .rel("IS_" + it, "self"))
        }

        if (hasAttributes || hasRoles || selfId == null) {
            def result = qb.insert(patterns).execute()
            def savedNode = result.get(0)
            setData(CodeStructureObserver.SELF_ID, selfId = savedNode.get("self").id().value)
        }

        relationships.each {
            def otherSelfId = it.value.getData(CodeStructureObserver.SELF_ID)
            if (otherSelfId == null) {
                it.value.save(qb)
                otherSelfId = it.value.getData(CodeStructureObserver.SELF_ID)
            }

            if (otherSelfId != null) {
                qb.match(
                        var("self").id(ConceptId.of(selfId)),
                        var("other").id(ConceptId.of(otherSelfId))
                ).insert(
                        var().isa(it.key.relationshipType)
                                .rel(it.key.selfRole, "self")
                                .rel(it.key.otherRole, "other")
                ).execute()
            }
        }

        //reset data
        attributes.clear()
        roles.clear()
        relationships.clear()
    }

    def <M> M getData(final DataKey<M> key) {
        return (M) data.get(key)
    }

    def <M> void setData(DataKey<M> key, M object) {
        data.put(key, object)
    }

    @Canonical
    static class NodeRelationship {
        String relationshipType
        String selfRole
        String otherRole

        NodeRelationship(String relationshipType) {
            this.relationshipType = relationshipType
            this.selfRole = "is_$relationshipType"
            this.otherRole = "has_$relationshipType"
        }

        NodeRelationship(String relationshipType, String selfRole, String otherRole) {
            this.relationshipType = relationshipType
            this.selfRole = selfRole
            this.otherRole = otherRole
        }
    }
}
