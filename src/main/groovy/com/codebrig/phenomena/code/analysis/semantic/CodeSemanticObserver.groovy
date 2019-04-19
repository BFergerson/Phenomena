package com.codebrig.phenomena.code.analysis.semantic

import com.codebrig.omnisrc.SourceLanguage
import com.codebrig.omnisrc.SourceNodeFilter
import com.codebrig.omnisrc.observe.filter.LanguageFilter
import com.codebrig.omnisrc.observe.filter.MultiFilter
import com.codebrig.omnisrc.observe.filter.TypeFilter
import com.codebrig.omnisrc.observe.filter.WildcardFilter
import com.codebrig.phenomena.code.CodeObserver
import com.codebrig.phenomena.code.ContextualNode
import com.google.common.base.Charsets
import com.google.common.base.Preconditions
import com.google.common.io.Resources
import gopkg.in.bblfsh.sdk.v1.uast.generated.Role
import scala.collection.JavaConverters

import java.util.stream.Collectors

/**
 * The CodeSemanticObserver creates additional edges between source code
 * and the semantic roles they play.
 *
 * Individual semantic roles creates edges, as such:
 *  - IS_VARIABLE
 *  - IS_ARGUMENT
 *  - IS_DECLARATION
 *
 * Actual semantic roles combine the individual semantic roles, as such:
 *  - IS_ARGUMENT_DECLARATION_VARIABLE
 *
 *  The actual semantic roles provide more specificity at the cost of disk space.
 *
 * @version 0.2.1
 * @since 0.2.1
 * @author <a href="mailto:brandon.fergerson@codebrig.com">Brandon Fergerson</a>
 */
class CodeSemanticObserver extends CodeObserver {

    private final SourceNodeFilter filter
    private final boolean includeIndividualSemanticRoles
    private final boolean includeActualSemanticRoles

    CodeSemanticObserver() {
        this(true, false)
    }

    CodeSemanticObserver(boolean includeIndividualSemanticRoles, boolean includeActualSemanticRoles) {
        this.filter = MultiFilter.matchAll(new WildcardFilter())
                .reject(getJavaRejectFilter())
        this.includeIndividualSemanticRoles = includeIndividualSemanticRoles
        this.includeActualSemanticRoles = includeActualSemanticRoles
        Preconditions.checkArgument(includeIndividualSemanticRoles || includeActualSemanticRoles,
                "Must include individual and/or actual semantic roles")
    }

    @Override
    void applyObservation(ContextualNode node, ContextualNode parentNode) {
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

    @Override
    SourceNodeFilter getFilter() {
        return filter
    }

    @Override
    String getSchema() {
        def structureSchema = ""
        if (includeIndividualSemanticRoles) {
            structureSchema += "\n" + SourceLanguage.OmniSRC.getIndividualSemanticRolesSchemaDefinition()
        }
        if (includeActualSemanticRoles) {
            structureSchema += "\n" + SourceLanguage.OmniSRC.getActualSemanticRolesSchemaDefinition()
        }
        return structureSchema
    }

    @Override
    String[] getRules() {
        return [Resources.toString(Resources.getResource(
                "rules/dependence/language/java/variable-declaration-fragment-hierarchy.gql"),
                Charsets.UTF_8)]
    }

    private static List<Role> getRoles(ContextualNode n) {
        return asJavaCollection(n.underlyingNode.roles()).stream()
                .collect(Collectors.toList())
    }

    private static <T> Collection<T> asJavaCollection(scala.collection.Iterable<T> scalaIterator) {
        return JavaConverters.asJavaCollectionConverter(scalaIterator).asJavaCollection()
    }

    private static SourceNodeFilter getJavaRejectFilter() {
        return MultiFilter.matchAll(new LanguageFilter(SourceLanguage.Java),
                new TypeFilter("VariableDeclarationFragment"))
    }
}
