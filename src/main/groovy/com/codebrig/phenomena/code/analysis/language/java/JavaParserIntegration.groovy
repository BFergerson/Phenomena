package com.codebrig.phenomena.code.analysis.language.java

import com.codebrig.arthur.observe.structure.naming.JavaNaming
import com.codebrig.phenomena.Phenomena
import com.codebrig.phenomena.code.ContextualNode
import com.github.javaparser.JavaParser
import com.github.javaparser.Position
import com.github.javaparser.Range
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.*
import com.github.javaparser.ast.comments.JavadocComment
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName
import com.github.javaparser.ast.stmt.ExpressionStmt
import com.github.javaparser.ast.type.ArrayType
import com.github.javaparser.ast.type.ClassOrInterfaceType
import com.github.javaparser.ast.type.Type
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver

import java.util.concurrent.ConcurrentHashMap

/**
 * Used to integrate JavaParser AST nodes
 *
 * @version 0.2.2
 * @since 0.2
 * @author <a href="mailto:brandon.fergerson@codebrig.com">Brandon Fergerson</a>
 */
class JavaParserIntegration {

    private CombinedTypeSolver typeSolver
    private Map<File, CompilationUnit> parsedFiles = new ConcurrentHashMap<>()

    JavaParserIntegration(Phenomena phenomena) {
        typeSolver = new CombinedTypeSolver()
        typeSolver.add(new ReflectionTypeSolver())

        phenomena.scanPath.each {
            def file = new File(it)
            if (file.isDirectory()) {
                typeSolver.add(new JavaParserTypeSolver(file))
            } else {
                typeSolver.add(new JavaParserTypeSolver(file.getParentFile()))
            }
        }
    }

    void reset() {
        parsedFiles.clear()
    }

    CompilationUnit parseFile(File sourceFile) {
        if (!parsedFiles.containsKey(sourceFile)) {
            parsedFiles.put(sourceFile, JavaParser.parse(sourceFile))
        }
        return parsedFiles.get(sourceFile)
    }

    CombinedTypeSolver getTypeSolver() {
        return typeSolver
    }

    static String toQualifiedName(ClassOrInterfaceDeclaration n) {
        //package + class
        def unit = n.getParentNode().get() as CompilationUnit
        if (unit.packageDeclaration.isPresent()) {
            return unit.packageDeclaration.get().nameAsString + "." + n.nameAsString
        } else {
            return n.nameAsString
        }
    }

    static String toQualifiedName(Type type) {
        def argumentSignature = new StringBuilder()
        if (type.isPrimitiveType()) {
            argumentSignature.append(type.asString())
        } else if (type.isArrayType()) {
            def array = (ArrayType) type
            argumentSignature.append(toQualifiedName(array.componentType)).append("[]")
        } else if (type.isVoidType()) {
            argumentSignature.append("void")
        } else if (type.isClassOrInterfaceType()) {
            argumentSignature.append(JavaNaming.getJavaQualifiedName(type.asString()))
        } else {
            throw new RuntimeException("unsupported: " + type + " Type: " + type.class.name)
        }
        return argumentSignature.toString()
    }

    static String toQualifiedName(CallableDeclaration n) {
        //package + class + method name + '(' fully qualified argument signature ')'
        def argumentSignature = new StringBuilder()
        if (n instanceof MethodDeclaration) {
            argumentSignature.append(n.signature.name)
        } else if (n instanceof ConstructorDeclaration) {
            argumentSignature.append("<init>")
        }

        argumentSignature.append("(")
        for (Type it : n.signature.parameterTypes) {
            argumentSignature.append(toQualifiedName(it)).append(",")
        }
        if (argumentSignature.toString().endsWith(",")) {
            argumentSignature.deleteCharAt(argumentSignature.length() - 1)
        }
        argumentSignature.append(")")

        return toQualifiedName(n.getParentNode().get() as ClassOrInterfaceDeclaration) + "." + argumentSignature.toString()
    }

    static String toClassQualifiedName(String qualifiedName) {
        if (qualifiedName.contains("(")) {
            return qualifiedName.substring(0, qualifiedName.substring(0, qualifiedName.indexOf("(")).lastIndexOf("."))
        } else {
            return qualifiedName
        }
    }

    static String getMethodSubsignature(CallableDeclaration n, String qualifiedName) {
        if (n instanceof MethodDeclaration) {
            return toQualifiedName(n.type) + " " +
                    qualifiedName.substring(qualifiedName.substring(0, qualifiedName.indexOf("(")).lastIndexOf(".") + 1)
        } else if (n instanceof ConstructorDeclaration) {
            return "void " + qualifiedName.substring(qualifiedName.substring(0, qualifiedName.indexOf("(")).lastIndexOf(".") + 1)
        } else {
            throw new UnsupportedOperationException("CallableDeclaration: " + n + " Qualified name: " + qualifiedName)
        }
    }

    static Node getNameNode(Node node) {
        Objects.requireNonNull(node)
        if (node instanceof SimpleName) {
            return node
        } else if (node instanceof NameExpr) {
            return node.name
        } else if (node instanceof NodeWithSimpleName) {
            return node.name
        } else if (node instanceof Name) {
            return node
        } else if (node instanceof VariableDeclarationExpr) {
            return node.variables.get(0).name //todo: shouldn't hardcore zero?
        } else if (node instanceof FieldDeclaration) {
            return node.variables.get(0).name //todo: shouldn't hardcore zero?
        } else {
            return null
        }
    }

    static Node getEquivalentNode(CompilationUnit compilationUnit, ContextualNode contextualNode) {
        Objects.requireNonNull(compilationUnit)
        Objects.requireNonNull(contextualNode)
        def range = toRange(contextualNode.underlyingNode.startPosition, contextualNode.underlyingNode.endPosition)
        def stack = new Stack<Node>()
        stack.push(compilationUnit)

        boolean pastCallableDeclaration = false
        def equivalentNode = null
        while (!stack.isEmpty() && equivalentNode == null) {
            def it = stack.pop()
            if (it.range.isPresent()) {
                def nodeRange = removeArrayFromRange(it, it.range.get())
                if (nodeRange.contains(range) || pastCallableDeclaration) {
                    it.childNodes.each {
                        stack.push(it)
                    }
                    if (it instanceof CallableDeclaration) {
                        pastCallableDeclaration = true
                    }
                }

                if (range.contains(nodeRange)) {
                    equivalentNode = checkNodeType(contextualNode, it)
                }
            }
        }
        return equivalentNode
    }

    private static Range removeArrayFromRange(Node node, Range range) {
        if (node instanceof JavadocComment) {
            return range
        }

        if (node.childNodes.isEmpty() && node.tokenRange.isPresent()
                && node.tokenRange.get().toString().contains("[")) {
            def fullArrayStr = node.tokenRange.get().toString()
            return new Range(range.begin, new Position(range.end.line,
                    range.end.column - (fullArrayStr.length() - (fullArrayStr.indexOf("[")))))
        } else {
            return range
        }
    }

    private static Node checkNodeType(ContextualNode contextualNode, Node node) {
        switch (contextualNode.internalType) {
            case "FieldDeclaration":
                if (node instanceof FieldDeclaration) {
                    return node
                }
                break
            case "MethodDeclaration":
                if (node instanceof CallableDeclaration) {
                    return node
                }
                break
            case "MethodInvocation":
                if (node instanceof MethodCallExpr) {
                    return node
                } else if (node instanceof ExpressionStmt) {
                    return node.expression
                }
                break
            case "SingleVariableDeclaration":
                if (node instanceof Parameter) {
                    return node
                } else if (node instanceof VariableDeclarationExpr) {
                    return node.variables.get(0) //todo: shouldn't hardcore zero?
                }
                break
            case "SimpleName":
                if (node instanceof SimpleName) {
                    return node
                } else if (node instanceof NameExpr) {
                    return node.name
                } else if (node instanceof NodeWithSimpleName) {
                    return node.name
                } else if (node instanceof Name) {
                    return node
                } else if (node instanceof TypeExpr) {
                    if (node.type instanceof ClassOrInterfaceType) {
                        return ((ClassOrInterfaceType) node.type).name
                    }
                } else if (node instanceof ExpressionStmt && node.expression instanceof NameExpr) {
                    return node.expression
                }
                break
            case "QualifiedName":
                if (node instanceof FieldAccessExpr) {
                    return node
                } else if (node instanceof Name) {
                    return node
                }
                break
            case "VariableDeclarationExpression":
                if (node instanceof VariableDeclarationExpr) {
                    return node
                }
                break
            case "VariableDeclarationFragment":
                if (node instanceof VariableDeclarator) {
                    return node
                } else if (node instanceof Parameter) {
                    return node
                }
                break
            case "VariableDeclarationStatement":
                if (node instanceof ExpressionStmt) {
                    return node.expression
                }
                break
        }
        return null
    }

    static Range toRange(gopkg.in.bblfsh.sdk.v1.uast.generated.Position startPosition,
                         gopkg.in.bblfsh.sdk.v1.uast.generated.Position endPosition) {
        def javaParserEnd = toJavaParserPosition(endPosition)
        return new Range(toJavaParserPosition(startPosition), javaParserEnd.withColumn(javaParserEnd.column - 1))
    }

    static Position toJavaParserPosition(gopkg.in.bblfsh.sdk.v1.uast.generated.Position position) {
        return Position.pos(position.line(), position.col())
    }
}
