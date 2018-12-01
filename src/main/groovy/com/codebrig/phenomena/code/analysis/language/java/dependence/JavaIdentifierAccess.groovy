package com.codebrig.phenomena.code.analysis.language.java.dependence

import com.codebrig.omnisrc.SourceLanguage
import com.codebrig.omnisrc.SourceNodeFilter
import com.codebrig.omnisrc.observe.filter.LanguageFilter
import com.codebrig.omnisrc.observe.filter.MultiFilter
import com.codebrig.omnisrc.observe.filter.RoleFilter
import com.codebrig.omnisrc.observe.filter.TypeFilter
import com.codebrig.phenomena.code.ContextualNode
import com.codebrig.phenomena.code.analysis.dependence.IdentifierAccessObserver
import com.codebrig.phenomena.code.analysis.language.java.JavaParserIntegration
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.Parameter
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserFieldDeclaration
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserParameterDeclaration
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserSymbolDeclaration
import com.google.common.base.Charsets
import com.google.common.io.Resources

/**
 * Creates edges between Java variable usages and their declarations
 *
 * @version 0.2
 * @since 0.2
 * @author <a href="mailto:brandon.fergerson@codebrig.com">Brandon Fergerson</a>
 */
class JavaIdentifierAccess extends IdentifierAccessObserver {

    private static final MultiFilter variableDeclarationFilter = MultiFilter.matchAll(
            new LanguageFilter(SourceLanguage.Java),
            new RoleFilter("DECLARATION"), new RoleFilter("VARIABLE"),
            new TypeFilter().reject("VariableDeclarationFragment")
    )
    private static final MultiFilter identifierFilter = MultiFilter.matchAll(
            new LanguageFilter(SourceLanguage.Java),
            new RoleFilter("IDENTIFIER")
    )
    private static final Map<Node, ContextualNode> contextualDeclarations = new IdentityHashMap<>()
    private final JavaParserIntegration integration

    JavaIdentifierAccess(JavaParserIntegration integration) {
        this.integration = Objects.requireNonNull(integration)
    }

    @Override
    void applyObservation(ContextualNode node, ContextualNode parentNode) {
        def unit = integration.parseFile(node.sourceFile)
        def range = JavaParserIntegration.toRange(node.underlyingNode.startPosition, node.underlyingNode.endPosition)
        def javaParserNode = JavaParserIntegration.getNameNodeAtRange(unit, range)

        if (variableDeclarationFilter.evaluate(node)) {
            contextualDeclarations.put(javaParserNode, node)
        } else if (javaParserNode instanceof NameExpr) {
            def nodeType = JavaParserFacade.get(integration.typeSolver).solve(javaParserNode)
            if (nodeType.isSolved()) {
                def declaration = nodeType.correspondingDeclaration
                if (declaration instanceof JavaParserSymbolDeclaration) {
                    def wrappedNode = declaration.wrappedNode
                    if (wrappedNode instanceof VariableDeclarator) {
                        def contextualDeclaration = contextualDeclarations.get(wrappedNode.name)
                        node.addRelationshipTo(contextualDeclaration, "identifier_access")
                    } else {
                        def contextualDeclaration = contextualDeclarations.get(wrappedNode)
                        node.addRelationshipTo(contextualDeclaration, "identifier_access")
                    }
                } else if (declaration instanceof JavaParserParameterDeclaration) {
                    def wrappedNode = declaration.wrappedNode
                    if (wrappedNode instanceof Parameter) {
                        def contextualDeclaration = contextualDeclarations.get(wrappedNode.name)
                        node.addRelationshipTo(contextualDeclaration, "identifier_access")
                    } else {
                        def contextualDeclaration = contextualDeclarations.get(wrappedNode)
                        node.addRelationshipTo(contextualDeclaration, "identifier_access")
                    }
                } else if (declaration instanceof JavaParserFieldDeclaration) {
                    def contextualDeclaration = contextualDeclarations.get(declaration.wrappedNode)
                    node.addRelationshipTo(contextualDeclaration, "identifier_access")
                } else {
                    throw new UnsupportedOperationException("Unsupported declaration type: " + declaration)
                }
            }
        }
    }

    @Override
    void reset() {
        contextualDeclarations.clear()
        integration.reset()
    }

    @Override
    SourceNodeFilter getFilter() {
        return MultiFilter.matchAny(variableDeclarationFilter, identifierFilter)
    }

    @Override
    String getSchema() {
        return super.getSchema() + " " + Resources.toString(Resources.getResource(
                "schema/dependence/language/java/identifier-access-schema.gql"), Charsets.UTF_8)
    }
}
