package com.codebrig.phenomena.code.structure

import com.codebrig.arthur.SourceLanguage
import com.codebrig.arthur.observe.ObservedLanguage
import com.codebrig.arthur.observe.structure.StructureFilter
import com.codebrig.arthur.observe.structure.StructureLiteral
import com.codebrig.arthur.observe.structure.filter.WildcardFilter
import com.codebrig.phenomena.code.CodeObserver
import com.codebrig.phenomena.code.ContextualNode
import com.codebrig.phenomena.code.structure.key.SelfIdKey
import org.apache.commons.text.StringEscapeUtils
import scala.collection.JavaConverters

/**
 * The code structure observer creates nodes and edges which contain
 * the structure of the source code in the form of an abstract syntax graph.
 *
 * @version 0.2.4
 * @since 0.1
 * @author <a href="mailto:brandon.fergerson@codebrig.com">Brandon Fergerson</a>
 */
class CodeStructureObserver extends CodeObserver {

    static final Set<String> literalAttributes = StructureLiteral.allLiteralAttributes.keySet()
    static final SelfIdKey SELF_ID = new SelfIdKey()
    private final StructureFilter filter

    CodeStructureObserver() {
        this(new WildcardFilter())
    }

    CodeStructureObserver(StructureFilter filter) {
        this.filter = Objects.requireNonNull(filter)
    }

    @Override
    void applyObservation(ContextualNode node, ContextualNode parentNode) {
        if (!node.token.isEmpty()) {
            if (node.isLiteralNode()) {
                def literalAttribute = node.getLiteralAttribute()
                switch (literalAttribute) {
                    case StructureLiteral.booleanValueLiteral():
                        node.hasAttribute(literalAttribute, Boolean.valueOf(node.token))
                        break
                    case StructureLiteral.numberValueLiteral():
                        node.hasAttribute(literalAttribute, node.language.structureLiteral.toLong(node.token))
                        break
                    case StructureLiteral.doubleValueLiteral():
                        node.hasAttribute(literalAttribute, node.language.structureLiteral.toDouble(node.token))
                        break
                    default:
                        node.hasAttribute(literalAttribute, StringEscapeUtils.escapeJava(node.token)) //treat as string
                }
            } else {
                def token = node.token
                if (token.length() >= 2 && token[0] == '"' && token[token.length() - 1] == '"') {
                    token = token.substring(1, token.length() - 1)
                }
                node.hasAttribute("token", token)
            }
        }
        if (node.language.structureNaming.isNamedNodeType(node)) {
            node.hasAttribute("name", node.getName())
        }

        def attributes = JavaConverters.mapAsJavaMapConverter(node.underlyingNode.properties()).asJava()
        attributes.keySet().stream().filter({
            it != "internalRole" && it != "token" && it != "unescapedValue"
        }).each {
            def attrName = ObservedLanguage.toValidAttribute(it)
            if (literalAttributes.contains(attrName)) {
                switch (attrName) {
                    case StructureLiteral.booleanValueLiteral():
                        node.hasAttribute(attrName, Boolean.valueOf(attributes.get(it)))
                        break
                    case StructureLiteral.numberValueLiteral():
                        node.hasAttribute(attrName, Long.valueOf(attributes.get(it)))
                        break
                    case StructureLiteral.doubleValueLiteral():
                        node.hasAttribute(attrName, Float.valueOf(attributes.get(it)))
                        break
                    default:
                        node.hasAttribute(attrName, StringEscapeUtils.escapeJava(attributes.get(it))) //treat as string
                }
            } else {
                attrName = attrName.substring(0, 1).toUpperCase() + attrName.substring(1)
                node.hasAttribute(node.language.key + attrName, attributes.get(it))
            }
        }

        if (parentNode != null) {
            if (!parentNode.underlyingNode.children().contains(node.underlyingNode)) {
                //parent and child don't relate in any way besides parent/child
                node.addRelationshipTo(parentNode, "parent_child_relation", "is_child", "is_parent")
            } else {
                def relation = node.language.key + "_" + ObservedLanguage.toValidRelation(
                        node.underlyingNode.properties().get("internalRole").get())
                def selfRole = "is_" + relation.substring(0, relation.length() - 8) + "role"
                def otherRole = "has_" + relation.substring(0, relation.length() - 8) + "role"
                node.addRelationshipTo(parentNode, relation, selfRole, otherRole)
            }
        }
    }

    @Override
    StructureFilter getFilter() {
        return filter
    }

    @Override
    String getSchema() {
        return SourceLanguage.Omnilingual.getBaseStructureSchemaDefinition()
    }
}
