package com.codebrig.phenomena.code.analysis.language.java.dependence

import com.codebrig.omnisrc.SourceLanguage
import com.codebrig.omnisrc.SourceNodeFilter
import com.codebrig.omnisrc.observe.filter.LanguageFilter
import com.codebrig.omnisrc.observe.filter.MultiFilter
import com.codebrig.omnisrc.observe.filter.TypeFilter
import com.codebrig.phenomena.code.ContextualNode
import com.codebrig.phenomena.code.analysis.dependence.MethodCallObserver
import com.codebrig.phenomena.code.analysis.language.java.JavaParserIntegration
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.CallableDeclaration
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.stmt.ExpressionStmt
import com.github.javaparser.resolution.UnsolvedSymbolException
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserEnumDeclaration
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserMethodDeclaration
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionMethodDeclaration
import com.google.common.base.Charsets
import com.google.common.io.Resources

/**
 * Creates edges between Java method call statements and the methods they call
 *
 * @version 0.2.1
 * @since 0.2
 * @author <a href="mailto:brandon.fergerson@codebrig.com">Brandon Fergerson</a>
 */
class JavaMethodCallObserver extends MethodCallObserver {

    private static final Map<Node, ContextualNode> methodNodes = new IdentityHashMap<>()
    private static final Map<Node, ContextualNode> methodInvocations = new IdentityHashMap<>()
    private final JavaParserIntegration integration

    JavaMethodCallObserver(JavaParserIntegration integration) {
        this.integration = integration
    }

    @Override
    void applyObservation(ContextualNode node, ContextualNode parentNode) {
        def unit = integration.parseFile(node.sourceFile)
        def javaParserNode = JavaParserIntegration.getEquivalentNode(unit, node)

        if (javaParserNode instanceof CallableDeclaration) {
            if (methodInvocations.containsKey(javaParserNode)) {
                def methodInvocation = methodInvocations.get(javaParserNode)
                methodInvocation.addRelationshipTo(node, "method_call")
            }
            methodNodes.put(javaParserNode, node)
            return
        } else if (javaParserNode instanceof ExpressionStmt) {
            javaParserNode = javaParserNode.getExpression() as MethodCallExpr
        }

        MethodCallExpr methodCallExpr = javaParserNode as MethodCallExpr
        try {
            def nodeType = JavaParserFacade.get(integration.typeSolver).solve(methodCallExpr)
            if (nodeType.isSolved()) {
                def declaration = nodeType.correspondingDeclaration
                if (declaration instanceof JavaParserMethodDeclaration) {
                    def method = declaration.wrappedNode
                    if (methodNodes.containsKey(method)) {
                        def calledMethod = methodNodes.get(method)
                        node.addRelationshipTo(calledMethod, "method_call")
                    } else {
                        methodInvocations.put(method, node)
                    }
                } else if (declaration instanceof ReflectionMethodDeclaration
                        || declaration instanceof JavaParserEnumDeclaration.ValuesMethod) {
                    //can't solve these
                } else {
                    throw new IllegalArgumentException("Unsupported declaration: " + declaration.qualifiedName)
                }
            }
        } catch (UnsupportedOperationException | RuntimeException ex) {
            //ignore; JavaParser developers haven't gotten to it yet
        } catch (UnsolvedSymbolException ex) {
            //ignore; external method call
        }
    }

    @Override
    void reset() {
        methodNodes.clear()
        methodInvocations.clear()
        integration.reset()
    }

    @Override
    SourceNodeFilter getFilter() {
        return MultiFilter.matchAll(new LanguageFilter(SourceLanguage.Java),
                new TypeFilter("MethodDeclaration", "MethodInvocation"))
    }

    @Override
    String getSchema() {
        return super.getSchema() + " " + Resources.toString(Resources.getResource(
                "schema/dependence/language/java/method-call-schema.gql"), Charsets.UTF_8)
    }
}
