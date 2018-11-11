package com.codebrig.phenomena.code.analysis.language.java.dependence

import com.codebrig.omnisrc.SourceFilter
import com.codebrig.omnisrc.SourceLanguage
import com.codebrig.omnisrc.schema.filter.LanguageFilter
import com.codebrig.omnisrc.schema.filter.MultiFilter
import com.codebrig.omnisrc.schema.filter.WhitelistRoleFilter
import com.codebrig.phenomena.code.ContextualNode
import com.codebrig.phenomena.code.analysis.dependence.IdentifierAccessObserver
import com.codebrig.phenomena.code.analysis.language.java.JavaParserIntegration
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserFieldDeclaration
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserParameterDeclaration
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserSymbolDeclaration
import com.google.common.base.Charsets
import com.google.common.io.Resources

/**
 * todo: description
 *
 * @version 0.2
 * @since 0.2
 * @author <a href="mailto:brandon.fergerson@codebrig.com">Brandon Fergerson</a>
 */
class JavaIdentifierAccess extends IdentifierAccessObserver {

    private static final MultiFilter variableDeclarationFilter = MultiFilter.matchAll(new LanguageFilter(SourceLanguage.Java),
            new WhitelistRoleFilter("DECLARATION"), new WhitelistRoleFilter("VARIABLE"))
    private static final MultiFilter identifierFilter = MultiFilter.matchAll(new LanguageFilter(SourceLanguage.Java),
            new WhitelistRoleFilter("IDENTIFIER"))
    private static final Map<Node, ContextualNode> contextualDeclarations = new IdentityHashMap<>()
    private final JavaParserIntegration integration

    JavaIdentifierAccess(JavaParserIntegration integration) {
        this.integration = integration
    }

    @Override
    void applyObservation(ContextualNode node, ContextualNode parentNode, ContextualNode previousNode) {
        def unit = integration.parseFile(node.sourceFile)
        def range = JavaParserIntegration.toRange(node.underlyingNode.startPosition, node.underlyingNode.endPosition)
        def javaParserNode = JavaParserIntegration.getNameNodeAtRange(unit, range)

        if (variableDeclarationFilter.evaluate(node)) {
            contextualDeclarations.put(javaParserNode, node)
            return
        }

        if (javaParserNode instanceof NameExpr) {
            def nodeType = JavaParserFacade.get(integration.typeSolver).solve(javaParserNode)
            if (nodeType.isSolved()) {
                def declaration = nodeType.correspondingDeclaration
                if (declaration instanceof JavaParserSymbolDeclaration) {
                    def contextualDeclaration = contextualDeclarations.get(declaration.wrappedNode)
                    node.addRelationshipTo(contextualDeclaration, "identifier_access")
                } else if (declaration instanceof JavaParserParameterDeclaration) {
                    def contextualDeclaration = contextualDeclarations.get(declaration.wrappedNode)
                    node.addRelationshipTo(contextualDeclaration, "identifier_access")
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
    SourceFilter getFilter() {
        return MultiFilter.matchAny(variableDeclarationFilter, identifierFilter)
    }

    @Override
    String getSchema() {
        return super.getSchema() + " " +
                Resources.toString(Resources.getResource(
                        "schema/dependence/language/java/identifier-access-schema.gql"), Charsets.UTF_8)
    }
}
