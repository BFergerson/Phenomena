package com.codebrig.phenomena.code.analysis.language.java

import com.codebrig.phenomena.Phenomena
import com.github.javaparser.JavaParser
import com.github.javaparser.Position
import com.github.javaparser.Range
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.CallableDeclaration
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.ConstructorDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.Name
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.SimpleName
import com.github.javaparser.ast.type.ArrayType
import com.github.javaparser.ast.type.Type
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver

/**
 * todo: description
 *
 * @version 0.2
 * @since 0.2
 * @author <a href="mailto:brandon.fergerson@codebrig.com">Brandon Fergerson</a>
 */
class JavaParserIntegration {

    private CombinedTypeSolver typeSolver
    private Map<File, CompilationUnit> parsedFiles = new HashMap<>()

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
            if ("String" == type.asString()) {
                argumentSignature.append("java.lang.String")
            } else if ("Object" == type.asString()) {
                argumentSignature.append("java.lang.Object")
            } else if ("Double" == type.asString()) {
                argumentSignature.append("java.lang.Double")
            } else {
                argumentSignature.append(type as String)
                //throw new RuntimeException("unsupported: " + it + " Type: " + it.class.name)
            }
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

    static Node getNodeAtRange(Node n, Range r) {
        def foundNode
        n.childNodes.each {
            if (foundNode == null && it.range.isPresent()) {
                def range = it.range.get()
                if (range.begin == r.begin) {
                    if (it instanceof SimpleName || it instanceof Name || it instanceof NameExpr) {
                        foundNode = it.parentNode.get()
                    } else {
                        foundNode = it
                    }
                } else if (range.contains(r)) {
                    foundNode = getNodeAtRange(it, r)
                }
            }
        }
        return foundNode
    }

    static Node getNameNodeAtRange(Node n, Range r) {
        def foundNode
        n.childNodes.each {
            if (foundNode == null && it.range.isPresent()) {
                def range = it.range.get()
                if (range.begin == r.begin) {
                    foundNode = it
                } else if (range.contains(r)) {
                    foundNode = getNameNodeAtRange(it, r)
                }
            }
        }
        return foundNode
    }

    static Range toRange(gopkg.in.bblfsh.sdk.v1.uast.generated.Position startPosition,
                         gopkg.in.bblfsh.sdk.v1.uast.generated.Position endPosition) {
        return new Range(toJavaParserPosition(startPosition), toJavaParserPosition(endPosition))
    }

    static Position toJavaParserPosition(gopkg.in.bblfsh.sdk.v1.uast.generated.Position position) {
        return Position.pos(position.line(), position.col())
    }
}
