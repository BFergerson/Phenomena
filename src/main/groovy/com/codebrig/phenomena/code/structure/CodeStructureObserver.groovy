package com.codebrig.phenomena.code.structure

import com.codebrig.omnisrc.SourceLanguage
import com.codebrig.omnisrc.SourceNodeFilter
import com.codebrig.omnisrc.observe.ObservedLanguage
import com.codebrig.omnisrc.observe.filter.WildcardFilter
import com.codebrig.omnisrc.observe.structure.StructureLiteral
import com.codebrig.phenomena.code.CodeObserver
import com.codebrig.phenomena.code.ContextualNode
import com.codebrig.phenomena.code.structure.key.SelfIdKey
import gopkg.in.bblfsh.sdk.v1.uast.generated.Role
import scala.collection.JavaConverters

import java.util.stream.Collectors

/**
 * The code structure observer creates nodes and edges which contain
 * the structure of the source code in the form of an abstract syntax graph.
 *
 * @version 0.2
 * @since 0.1
 * @author <a href="mailto:brandon.fergerson@codebrig.com">Brandon Fergerson</a>
 */
class CodeStructureObserver implements CodeObserver {

    static final Set<String> literalAttributes = StructureLiteral.allLiteralAttributes.keySet()
    static final SelfIdKey SELF_ID = new SelfIdKey()
    private final SourceNodeFilter filter
    private boolean includeIndividualSemanticRoles
    private boolean includeActualSemanticRoles

    CodeStructureObserver() {
        this(new WildcardFilter())
    }

    CodeStructureObserver(SourceNodeFilter filter) {
        this.filter = Objects.requireNonNull(filter)
        this.includeIndividualSemanticRoles = true
        this.includeActualSemanticRoles = false
    }

    @Override
    void applyObservation(ContextualNode node, ContextualNode parentNode, ContextualNode previousNode) {
        node.setEntityType(getEntityType(node))
        if (!node.token.isEmpty()) {
            if (node.isLiteralNode()) {
                def literalAttribute = node.getLiteralAttribute()
                switch (literalAttribute) {
                    case StructureLiteral.booleanValueLiteral():
                        node.hasAttribute(literalAttribute, Boolean.valueOf(node.token))
                        break
                    case StructureLiteral.numberValueLiteral():
                        node.hasAttribute(literalAttribute, Long.valueOf(node.token))
                        break
                    case StructureLiteral.floatValueLiteral():
                        node.hasAttribute(literalAttribute, Float.valueOf(node.token))
                        break
                    default:
                        throw new UnsupportedOperationException(literalAttribute)
                }
            } else {
                node.hasAttribute("token", node.token)
            }
        }
        def attributes = asJavaMap(node.underlyingNode.properties())
        attributes.keySet().stream().filter({ it != "internalRole" && it != "token" }).each {
            def attrName = ObservedLanguage.toValidAttribute(it)
            if (literalAttributes.contains(attrName)) {
                switch (attrName) {
                    case StructureLiteral.booleanValueLiteral():
                        node.hasAttribute(attrName, Boolean.valueOf(attributes.get(it)))
                        break
                    case StructureLiteral.numberValueLiteral():
                        node.hasAttribute(attrName, Long.valueOf(attributes.get(it)))
                        break
                    case StructureLiteral.floatValueLiteral():
                        node.hasAttribute(attrName, Float.valueOf(attributes.get(it)))
                        break
                    default:
                        throw new UnsupportedOperationException(attrName)
                }
            } else {
                attrName = attrName.substring(0, 1).toUpperCase() + attrName.substring(1)
                node.hasAttribute(node.language.key + attrName, attributes.get(it))
            }
        }

        if (parentNode != null) {
            if (previousNode != null && previousNode != parentNode &&
                    previousNode.underlyingNode.children().contains(parentNode.underlyingNode)) {
                //parent and child don't relate in any way besides parent/child
                node.addRelationshipTo(previousNode, "parent_child_relation", "is_child", "is_parent")
            } else {
                def relation = node.language.key + "_" + ObservedLanguage.toValidRelation(node.underlyingNode.properties().get("internalRole").get())
                def selfRole = "is_" + relation.substring(0, relation.length() - 8) + "role"
                def otherRole = "has_" + relation.substring(0, relation.length() - 8) + "role"
                node.addRelationshipTo(parentNode, relation, selfRole, otherRole)
            }
        }

        def roleList = getRoles(node)
        if (includeIndividualSemanticRoles) {
            roleList.each {
                node.playsRole(it.name())
            }
        }
        if (includeActualSemanticRoles && roleList.size() > 1) {
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

            def actualRole = sb.toString()
            node.playsRole(actualRole)
        }
    }

    boolean getIncludeIndividualSemanticRoles() {
        return includeIndividualSemanticRoles
    }

    void setIncludeIndividualSemanticRoles(boolean includeIndividualSemanticRoles) {
        this.includeIndividualSemanticRoles = includeIndividualSemanticRoles
    }

    boolean getIncludeActualSemanticRoles() {
        return includeActualSemanticRoles
    }

    void setIncludeActualSemanticRoles(boolean includeActualSemanticRoles) {
        this.includeActualSemanticRoles = includeActualSemanticRoles
    }

    private static String getEntityType(ContextualNode n) {
        if (n.underlyingNode.internalType().isEmpty()) {
            return n.underlyingNode.internalType() //todo: understand this
        }
        return n.language.qualifiedName + ObservedLanguage.toValidEntity(n.underlyingNode.internalType())
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
    SourceNodeFilter getFilter() {
        return filter
    }

    @Override
    String getSchema() {
        def fullSchema = SourceLanguage.OmniSRC.getBaseStructureSchemaDefinition()
        if (includeIndividualSemanticRoles) {
            fullSchema += "\n" + SourceLanguage.OmniSRC.getIndividualSemanticRolesSchemaDefinition()
        }
        if (includeActualSemanticRoles) {
            fullSchema += "\n" + SourceLanguage.OmniSRC.getActualSemanticRolesSchemaDefinition()
        }
        return fullSchema
    }
}
