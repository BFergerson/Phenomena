package com.codebrig.phenomena.code.analysis.semantic

import com.codebrig.arthur.SourceLanguage
import com.codebrig.arthur.observe.structure.StructureFilter
import com.codebrig.arthur.observe.structure.filter.LanguageFilter
import com.codebrig.arthur.observe.structure.filter.MultiFilter
import com.codebrig.arthur.observe.structure.filter.TypeFilter
import com.codebrig.arthur.observe.structure.filter.WildcardFilter
import com.codebrig.phenomena.code.CodeObserver
import com.codebrig.phenomena.code.ContextualNode
import com.google.common.base.Charsets
import com.google.common.io.Resources
import gopkg.in.bblfsh.sdk.v1.uast.generated.Role
import scala.collection.JavaConverters

import java.util.stream.Collectors

/**
 * The CodeSemanticObserver creates additional edges between
 * source code and the semantic roles they play.
 *
 * Semantic roles creates edges, as such:
 *  - IS_VARIABLE
 *  - IS_ARGUMENT
 *  - IS_DECLARATION
 *
 * @version 0.2.2
 * @since 0.2.1
 * @author <a href="mailto:brandon.fergerson@codebrig.com">Brandon Fergerson</a>
 */
class CodeSemanticObserver extends CodeObserver {

    private final StructureFilter filter

    CodeSemanticObserver() {
        this.filter = MultiFilter.matchAll(new WildcardFilter())
                .reject(getJavaRejectFilter())
    }

    @Override
    void applyObservation(ContextualNode node, ContextualNode parentNode) {
        def roleList = getRoles(node)
        roleList.each {
            node.playsRole(it.name())
        }
    }

    @Override
    StructureFilter getFilter() {
        return filter
    }

    @Override
    String getSchema() {
        return SourceLanguage.Omnilingual.getSemanticRolesSchemaDefinition()
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

    private static StructureFilter getJavaRejectFilter() {
        return MultiFilter.matchAll(new LanguageFilter(SourceLanguage.Java),
                new TypeFilter("VariableDeclarationFragment"))
    }
}
