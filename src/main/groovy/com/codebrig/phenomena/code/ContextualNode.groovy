package com.codebrig.phenomena.code

import ai.grakn.concept.ConceptId
import ai.grakn.graql.QueryBuilder
import ai.grakn.graql.VarPattern
import com.codebrig.omnisrc.SourceLanguage
import com.codebrig.omnisrc.SourceNode
import com.codebrig.phenomena.code.structure.CodeStructureObserver
import gopkg.in.bblfsh.sdk.v1.uast.generated.Node
import groovy.transform.Canonical

import static ai.grakn.graql.Graql.var

/**
 * todo: description
 *
 * @version 0.2
 * @since 0.1
 * @author <a href="mailto:brandon.fergerson@codebrig.com">Brandon Fergerson</a>
 */
class ContextualNode extends SourceNode {

    private final CodeObserverVisitor context
    private final Map<DataKey<?>, Object> data = new IdentityHashMap<>()
    private final Map<String, String> attributes = new HashMap<>()
    private final Map<NodeRelationship, ContextualNode> relationships = new HashMap<>()
    private final Set<String> roles = new HashSet<>()
    private String entityType

    ContextualNode(CodeObserverVisitor context, Node rootNode, SourceLanguage language, Node node) {
        super(language, rootNode, node)
        this.context = Objects.requireNonNull(context)
    }

    ContextualNode(CodeObserverVisitor context, SourceNode sourceNode) {
        super(sourceNode.language, sourceNode.rootNode, sourceNode.underlyingNode)
        this.context = Objects.requireNonNull(context)
    }

    String getEntityType() {
        return entityType
    }

    void setEntityType(String entityType) {
        this.entityType = entityType
    }

    void hasAttribute(String key, String value) {
        attributes.put(key, value)
    }

    void playsRole(String role) {
        roles.add(Objects.requireNonNull(role))
    }

    void addRelationshipTo(ContextualNode otherNode, String relationshipType) {
        addRelationshipTo(otherNode, relationshipType,
                "is_$relationshipType", "has_$relationshipType")
    }

    void addRelationshipTo(ContextualNode otherNode, String relationshipType, String rel1, String rel2) {
        relationships.put(new NodeRelationship(relationshipType, rel1, rel2), otherNode)
    }

    void save(QueryBuilder qb) {
        def patterns = new ArrayList<VarPattern>()
        def nodePattern = var("self").isa(entityType)
        attributes.each {
            nodePattern = nodePattern.has(it.key, it.value)
        }
        patterns.add(nodePattern)

        roles.each {
            patterns.add(var().isa(it)
                    .rel("IS_" + it, "self"))
        }
        def result = qb.insert(patterns).execute()
        def savedNode = result.get(0)
        def selfId = savedNode.get("self").id().value
        setData(CodeStructureObserver.SELF_ID, selfId)

        relationships.each {
            def parentId = it.value.getData(CodeStructureObserver.SELF_ID)
            qb.match(
                    var("self").id(ConceptId.of(selfId)),
                    var("parent").id(ConceptId.of(parentId))
            ).insert(
                    var().isa(it.key.relationshipType)
                            .rel(it.key.selfRole, "self")
                            .rel(it.key.parentRole, "parent")
            ).execute()
        }
    }

    def <M> M getData(final DataKey<M> key) {
        return (M) data.get(key)
    }

    def <M> void setData(DataKey<M> key, M object) {
        data.put(key, object)
    }

    @Canonical
    private static class NodeRelationship {
        String relationshipType
        String selfRole
        String parentRole

        NodeRelationship(String relationshipType, String selfRole, String parentRole) {
            this.relationshipType = relationshipType
            this.selfRole = selfRole
            this.parentRole = parentRole
        }
    }
}
