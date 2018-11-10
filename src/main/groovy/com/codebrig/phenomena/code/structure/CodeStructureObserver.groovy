package com.codebrig.phenomena.code.structure

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
    void applyObservation(ContextualNode node, ContextualNode parentNode, ContextualNode previousNode) {
        node.setEntityType(getEntityType(node))
        if (!getToken(node).isEmpty()) {
            node.hasAttribute("token", getToken(node))
        }
        def attributes = asJavaMap(node.underlyingNode.properties())
        attributes.keySet().stream().filter({ it != "internalRole" && it != "token" }).each {
            def attrName = ObservedLanguage.toValidAttribute(it)
            attrName = attrName.substring(0, 1).toUpperCase() + attrName.substring(1)
            node.hasAttribute(node.language.key() + attrName, attributes.get(it))
        }

        if (parentNode != null) {
            if (previousNode != null && previousNode != parentNode &&
                    previousNode.underlyingNode.children().contains(parentNode.underlyingNode)) {
                //parent and child don't relate in any way besides parent/child
                node.addRelationshipTo(previousNode, "parent_child_relation", "is_child", "is_parent")
            } else {
                def relation = ObservedLanguage.toValidRelation(node.underlyingNode.properties().get("internalRole").get())
                node.addRelationshipTo(parentNode, node.language.key() + "_" + relation)
            }
        }

        def roleList = getRoles(node)
        roleList.each {
            node.playsRole(it.name())
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
            node.playsRole(superRole)
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
