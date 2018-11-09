package com.codebrig.phenomena.code.structure

import ai.grakn.concept.ConceptId
import ai.grakn.graql.QueryBuilder
import com.codebrig.omnisrc.SourceFilter
import com.codebrig.omnisrc.SourceLanguage
import com.codebrig.omnisrc.observations.ObservedLanguage
import com.codebrig.omnisrc.schema.filter.WildcardFilter
import com.codebrig.phenomena.code.CodeObserver
import com.codebrig.phenomena.code.ContextualNode
import com.codebrig.phenomena.code.structure.key.SelfIdKey
import gopkg.in.bblfsh.sdk.v1.uast.generated.Role
import scala.collection.JavaConverters

import java.util.stream.Collectors

import static ai.grakn.graql.Graql.var

/**
 * todo: description
 *
 * @version 0.2
 * @since 0.1
 * @author <a href="mailto:brandon.fergerson@codebrig.com">Brandon Fergerson</a>
 */
class CodeStructureObserver implements CodeObserver {

    static final SelfIdKey SELF_ID = new SelfIdKey()
    private final SourceFilter filter

    CodeStructureObserver() {
        this(new WildcardFilter())
    }

    CodeStructureObserver(SourceFilter filter) {
        this.filter = Objects.requireNonNull(filter)
    }

    @Override
    void applyObservation(QueryBuilder qb, ContextualNode node,
                          ContextualNode parentNode, ContextualNode previousNode) {
        def nodePattern = var("node").isa(getEntityType(node))
        if (!getToken(node).isEmpty()) {
            nodePattern = nodePattern.has("token", getToken(node))
        }
        def attributes = asJavaMap(node.underlyingNode.properties())
        attributes.keySet().stream().filter({ it != "internalRole" && it != "token" }).each {
            def attrName = ObservedLanguage.toValidAttribute(it)
            attrName = attrName.substring(0, 1).toUpperCase() + attrName.substring(1)
            nodePattern = nodePattern.has(node.language.key() + attrName, attributes.get(it))
        }
        def savedNode = qb.insert(nodePattern).execute().get(0)
        node.setData(SELF_ID, savedNode.get("node").id().value)
        def selfId = node.getData(SELF_ID)

        if (parentNode != null) {
            if (previousNode != null && previousNode != parentNode &&
                    previousNode.underlyingNode.children().contains(parentNode.underlyingNode)) {
                //parent and child don't relate in any way besides parent/child
                def parentId = previousNode.getData(SELF_ID)
                qb.match(
                        var("parent").id(ConceptId.of(parentId)),
                        var("child").id(ConceptId.of(selfId))
                ).insert(
                        var().isa("parent_child_relation")
                                .rel("is_parent", "parent")
                                .rel("is_child", "child")
                ).execute()
            } else {
                def parentId = parentNode.getData(SELF_ID)
                def relation = ObservedLanguage.toValidRelation(node.underlyingNode.properties().get("internalRole").get())
                qb.match(
                        var("parent").id(ConceptId.of(parentId)),
                        var("child").id(ConceptId.of(selfId))
                ).insert(
                        var().isa(node.language.key() + "_" + relation)
                                .rel("has_" + node.language.key() + "_$relation", "parent")
                                .rel("is_" + node.language.key() + "_$relation", "child")
                ).execute()
            }
        }

        def roleList = getRoles(node)
        roleList.each {
            qb.match(
                    var("self").id(ConceptId.of(selfId))
            ).insert(
                    var().isa(it.name())
                            .rel("IS_" + it.name(), "self")
            ).execute()
        }

        if (roleList.size() > 1) {
            //add merged super role
            def sb = new StringBuilder()
            def alphaSortRoles = new ArrayList<String>(roleList.stream().map({
                it.name()
            }).collect(Collectors.toList()))
            alphaSortRoles.sort(String.CASE_INSENSITIVE_ORDER)
            for (int i = 0; i < alphaSortRoles.size(); i++) {
                sb.append(alphaSortRoles.get(i))
                if ((i + 1) < alphaSortRoles.size()) {
                    sb.append("_")
                }
            }

            def superRole = sb.toString()
            qb.match(
                    var("self").id(ConceptId.of(selfId))
            ).insert(
                    var().isa(superRole)
                            .rel("IS_" + superRole, "self")
            ).execute()
        }
    }

    private static String getEntityType(ContextualNode n) {
        if (n.underlyingNode.internalType().isEmpty()) {
            return n.underlyingNode.internalType() //todo: understand this
        }
        return n.language.qualifiedName + ObservedLanguage.toValidEntity(n.underlyingNode.internalType())
    }

    private static String getToken(ContextualNode n) {
        if (n.underlyingNode.properties().contains("token")) {
            return n.underlyingNode.properties().get("token").get()
        }
        return n.underlyingNode.token()
    }

    private static List<Role> getRoles(ContextualNode n) {
        return asJavaCollection(n.underlyingNode.roles()).stream()
                .collect(Collectors.toList())
    }

    private static <T> Collection<T> asJavaCollection(scala.collection.Iterable<T> scalaIterator) {
        return JavaConverters.asJavaCollectionConverter(scalaIterator).asJavaCollection()
    }

    private static Map<String, String> asJavaMap(scala.collection.Map<String, String> scalaMap) {
        return JavaConverters.mapAsJavaMapConverter(scalaMap).asJava()
    }

    @Override
    SourceFilter getFilter() {
        return filter
    }

    @Override
    String getSchema() {
        return SourceLanguage.OmniSRC.getFullSchemaDefinition("1.0")
    }
}
