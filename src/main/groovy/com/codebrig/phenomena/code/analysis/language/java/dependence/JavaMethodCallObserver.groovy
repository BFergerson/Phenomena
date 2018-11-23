package com.codebrig.phenomena.code.analysis.language.java.dependence

import com.codebrig.omnisrc.SourceNodeFilter
import com.codebrig.omnisrc.observe.filter.TypeFilter
import com.codebrig.phenomena.code.ContextualNode
import com.codebrig.phenomena.code.analysis.dependence.MethodCallObserver
import com.codebrig.phenomena.code.analysis.language.java.JavaParserIntegration
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.stmt.ExpressionStmt
import com.github.javaparser.resolution.UnsolvedSymbolException
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserMethodDeclaration
import com.google.common.base.Charsets
import com.google.common.io.Resources

/**
 * Creates edges between Java method call statements and the methods they call
 *
 * @version 0.2
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
    void applyObservation(ContextualNode node, ContextualNode parentNode, ContextualNode previousNode) {
        def unit = integration.parseFile(node.sourceFile)
        def range = JavaParserIntegration.toRange(node.underlyingNode.startPosition, node.underlyingNode.endPosition)
        def javaParserNode = JavaParserIntegration.getNodeAtRange(unit, range)

        if (javaParserNode instanceof MethodDeclaration) {
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
                } else {
                    throw new IllegalArgumentException("Unsupported declaration: " + declaration.qualifiedName)
                }
            }
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
        return new TypeFilter("MethodDeclaration", "MethodInvocation")
    }

    @Override
    String getSchema() {
        return super.getSchema() + " " +
                Resources.toString(Resources.getResource(
                        "schema/dependence/language/java/method-call-schema.gql"), Charsets.UTF_8)
    }
}
