package com.codebrig.phenomena.code.analysis.language.java.dependence

import com.codebrig.omnisrc.SourceLanguage
import com.codebrig.omnisrc.SourceNodeFilter
import com.codebrig.omnisrc.observe.filter.*
import com.codebrig.phenomena.code.ContextualNode
import com.codebrig.phenomena.code.analysis.dependence.IdentifierAccessObserver
import com.codebrig.phenomena.code.analysis.language.java.JavaParserIntegration
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.Parameter
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.SimpleName
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName
import com.github.javaparser.resolution.UnsolvedSymbolException
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.*
import com.github.javaparser.symbolsolver.model.resolution.SymbolReference
import com.google.common.base.Charsets
import com.google.common.io.Resources

/**
 * Creates edges between Java variable usages and their declarations
 *
 * @version 0.2.2
 * @since 0.2
 * @author <a href="mailto:brandon.fergerson@codebrig.com">Brandon Fergerson</a>
 */
class JavaIdentifierAccessObserver extends IdentifierAccessObserver {

    private static final MultiFilter variableDeclarationFilter = MultiFilter.matchAll(
            new RoleFilter("DECLARATION"), new RoleFilter("VARIABLE")
    )
    private static final MultiFilter functionArgumentFilter = MultiFilter.matchAll(
            new RoleFilter("FUNCTION"), new RoleFilter("ARGUMENT")
    )
    private static final RoleFilter identifierFilter =
            new RoleFilter("IDENTIFIER")
    private static final TypeFilter variableDeclarationFragmentFilter =
            new TypeFilter("VariableDeclarationFragment")
    private final Map<Node, ContextualNode> contextualDeclarations = new IdentityHashMap<>()
    private final Map<Node, List<ContextualNode>> contextualIdentifiers = new IdentityHashMap<>()
    private final JavaParserIntegration integration

    JavaIdentifierAccessObserver(JavaParserIntegration integration) {
        this.integration = Objects.requireNonNull(integration)
    }

    @Override
    void applyObservation(ContextualNode node, ContextualNode parentNode) {
        //ensures when declaring multiple variables on a single line that all are visited during that pass
        new TypeFilter("VariableDeclarationFragment").getFilteredNodes(node.children).each {
            applyObservation(codeObserverVisitor.getOrCreateContextualNode(it, node.sourceFile), node)
        }

        try {
            Node javaParserDeclaration = null
            def unit = integration.parseFile(node.sourceFile)
            def javaParserNode = JavaParserIntegration.getEquivalentNode(unit, node)

            if (variableDeclarationFilter.evaluate(node)
                    || functionArgumentFilter.evaluate(node)
                    || variableDeclarationFragmentFilter.evaluate(node)) {
                javaParserDeclaration = JavaParserIntegration.getNameNode(javaParserNode)
                def contextualDeclarationName = codeObserverVisitor.getOrCreateContextualNode(
                        new SimpleNameFilter(javaParserDeclaration.toString()).getFilteredNodes(node).next(), node.sourceFile)
                contextualDeclarations.put(javaParserDeclaration, contextualDeclarationName)
            } else if (javaParserNode instanceof SimpleName) {
                javaParserDeclaration = getDeclarationName(JavaParserFacade.get(integration.typeSolver).solve(javaParserNode))
                if (javaParserDeclaration != null) {
                    contextualIdentifiers.putIfAbsent(javaParserDeclaration, new ArrayList<>())
                    contextualIdentifiers.get(javaParserDeclaration).add(node)
                }
            } else if (javaParserNode instanceof NodeWithSimpleName) {
                javaParserDeclaration = getDeclarationName(JavaParserFacade.get(integration.typeSolver).solve(javaParserNode.name))
                if (javaParserDeclaration != null) {
                    contextualIdentifiers.putIfAbsent(javaParserDeclaration, new ArrayList<>())
                    contextualIdentifiers.get(javaParserDeclaration).add(node)
                }
            }

            if (javaParserDeclaration != null && contextualDeclarations.containsKey(javaParserDeclaration)
                    && contextualIdentifiers.containsKey(javaParserDeclaration)) {
                def declarationNode = contextualDeclarations.get(javaParserDeclaration)
                contextualIdentifiers.get(javaParserDeclaration).removeIf({
                    if (it != declarationNode) {
                        it.addRelationshipTo(declarationNode, "identifier_access")
                    }
                    return true
                })
            }
        } catch (UnsolvedSymbolException | UnsupportedOperationException ex) {
            //ignore; can't be solved
        }
    }

    private static Node getDeclarationName(SymbolReference nodeType) {
        if (nodeType.isSolved()) {
            def declaration = nodeType.correspondingDeclaration
            if (declaration instanceof JavaParserSymbolDeclaration) {
                def wrappedNode = declaration.wrappedNode
                if (wrappedNode instanceof VariableDeclarator) {
                    return wrappedNode.name
                } else {
                    return wrappedNode
                }
            } else if (declaration instanceof JavaParserParameterDeclaration) {
                def wrappedNode = declaration.wrappedNode
                if (wrappedNode instanceof Parameter) {
                    return wrappedNode.name
                } else {
                    return wrappedNode
                }
            } else if (declaration instanceof JavaParserFieldDeclaration) {
                return declaration.wrappedNode.variables.get(0).name
            } else if (declaration instanceof JavaParserEnumConstantDeclaration) {
                return declaration.wrappedNode.name
            } else if ((ResolvedFieldDeclaration) declaration) {
                if (declaration.isField()
                        && declaration.asField().declaringType() instanceof JavaParserClassDeclaration) {
                    return ((JavaParserClassDeclaration) declaration.asField().declaringType()).wrappedNode.name
                }
                throw new UnsupportedOperationException("Unsupported declaration type: " + declaration)
            } else {
                throw new UnsupportedOperationException("Unsupported declaration type: " + declaration)
            }
        }
        return null
    }

    @Override
    void reset() {
        contextualDeclarations.clear()
        integration.reset()
    }

    @Override
    SourceNodeFilter getFilter() {
        return MultiFilter.matchAll(new LanguageFilter(SourceLanguage.Java),
                MultiFilter.matchAny(variableDeclarationFilter, identifierFilter))
    }

    @Override
    String getSchema() {
        return super.getSchema() + " " + Resources.toString(Resources.getResource(
                "schema/dependence/language/java/identifier-access-schema.gql"), Charsets.UTF_8)
    }
}
