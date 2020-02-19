package com.codebrig.phenomena.code.analysis.semantic

import com.codebrig.arthur.SourceLanguage
import com.codebrig.arthur.observe.structure.filter.TypeFilter
import com.codebrig.phenomena.Phenomena
import com.codebrig.phenomena.code.CodeObserverVisitor
import com.codebrig.phenomena.code.structure.CodeStructureObserver
import groovy.util.logging.Slf4j
import org.junit.Test

import static org.junit.Assert.*

@Slf4j
class CodeSemanticObserverTest {

    @Test
    void skipVariableDeclarationFragment_noSave() {
        def file = new File(".", "/src/test/resources/java/ForStmt.java")
        def language = SourceLanguage.getSourceLanguage(file)
        def phenomena = new Phenomena()
        def visitor = new CodeObserverVisitor()
        visitor.addObserver(new CodeStructureObserver())
        visitor.addObserver(new CodeSemanticObserver())
        phenomena.setupVisitor(visitor)
        phenomena.connectToBabelfish()
        def processedFile = phenomena.processSourceFile(file, language)

        def foundCompilationUnit = false
        def foundVariableDeclarationFragment = false
        new TypeFilter("CompilationUnit")
                .getFilteredNodes(language, processedFile.parseResponse.uast).each {
            foundCompilationUnit = true
            def contextualNode = visitor.getContextualNode(it.underlyingNode)
            assertEquals(1, contextualNode.getPlayedRoles().size())
            assertEquals("FILE", contextualNode.getPlayedRoles()[0])
        }
        new TypeFilter("VariableDeclarationFragment")
                .getFilteredNodes(language, processedFile.parseResponse.uast).each {
            foundVariableDeclarationFragment = true
            def contextualNode = visitor.getContextualNode(it.underlyingNode)
            assertEquals(0, contextualNode.getPlayedRoles().size())
        }
        assertTrue(foundCompilationUnit)
        assertTrue(foundVariableDeclarationFragment)
        phenomena.close()
    }

    @Test
    void skipVariableDeclarationFragment_withSave() {
        def file = new File(".", "/src/test/resources/java/ForStmt.java")
        def language = SourceLanguage.getSourceLanguage(file)
        def phenomena = new Phenomena()
        phenomena.connectToBabelfish()
        phenomena.connectToGrakn()
        phenomena.setupVisitor(new CodeStructureObserver(), new CodeSemanticObserver())
        phenomena.setupOntology()
        def processedFile = phenomena.processSourceFile(file, language)
        assertNotNull(processedFile.rootNodeId)
        log.info processedFile.rootNodeId
        phenomena.close()
    }
}