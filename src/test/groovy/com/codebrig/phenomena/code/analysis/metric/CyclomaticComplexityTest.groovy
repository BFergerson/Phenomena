package com.codebrig.phenomena.code.analysis.metric

import com.codebrig.omnisrc.SourceLanguage
import com.codebrig.omnisrc.observe.filter.FunctionFilter
import com.codebrig.omnisrc.observe.filter.MultiFilter
import com.codebrig.omnisrc.observe.filter.NameFilter
import com.codebrig.phenomena.Phenomena
import com.codebrig.phenomena.PhenomenaTest
import com.codebrig.phenomena.code.CodeObserverVisitor
import com.codebrig.phenomena.code.structure.CodeStructureObserver
import org.junit.Test

import static org.junit.Assert.*

class CyclomaticComplexityTest extends PhenomenaTest {

    @Test
    void pythonInnerMethod_noSave() {
        def file = new File(".", "/src/test/resources/python/InnerMethodComplexity.py")
        def language = SourceLanguage.getSourceLanguage(file)
        def phenomena = new Phenomena()
        def visitor = new CodeObserverVisitor()
        visitor.addObserver(new CyclomaticComplexity())
        phenomena.setupVisitor(visitor)
        phenomena.connectToBabelfish()
        def processedFile = phenomena.processSourceFile(file, language)

        def foundOuter = false
        def foundInner = false
        MultiFilter.matchAll(new FunctionFilter(), new NameFilter("outer"))
                .getFilteredNodes(language, processedFile.parseResponse.uast).each {
            foundOuter = true
            def contextualNode = visitor.getContextualNode(it.underlyingNode)
            def cyclomaticComplexity = contextualNode.getAttributes().get("cyclomatic_complexity")
            assertEquals(1, cyclomaticComplexity)
        }
        MultiFilter.matchAll(new FunctionFilter(), new NameFilter("inner_increment"))
                .getFilteredNodes(language, processedFile.parseResponse.uast).each {
            foundInner = true
            def contextualNode = visitor.getContextualNode(it.underlyingNode)
            def cyclomaticComplexity = contextualNode.getAttributes().get("cyclomatic_complexity")
            assertEquals(2, cyclomaticComplexity)
        }
        assertTrue(foundOuter)
        assertTrue(foundInner)
    }

    @Test
    void pythonInnerMethod_withSave() {
        def file = new File(".", "/src/test/resources/python/InnerMethodComplexity.py")
        def language = SourceLanguage.getSourceLanguage(file)
        def phenomena = new Phenomena()
        phenomena.connectToBabelfish()
        phenomena.connectToGrakn()
        phenomena.setupVisitor(new CodeStructureObserver(), new CyclomaticComplexity())
        phenomena.setupOntology()
        def processedFile = phenomena.processSourceFile(file, language)
        assertNotNull(processedFile.rootNodeId)
        println processedFile.rootNodeId
    }

    @Test
    void javaCyclomaticComplexity_noSave() {
        def file = new File(".", "/src/test/resources/java/CyclomaticComplexity.java")
        def language = SourceLanguage.getSourceLanguage(file)
        def phenomena = new Phenomena()
        def visitor = new CodeObserverVisitor()
        visitor.addObserver(new CyclomaticComplexity())
        phenomena.setupVisitor(visitor)
        phenomena.connectToBabelfish()
        def processedFile = phenomena.processSourceFile(file, language)

        def foundCheckForError = false
        def foundIsReady = false
        MultiFilter.matchAll(new FunctionFilter(), new NameFilter("checkForError"))
                .getFilteredNodes(language, processedFile.parseResponse.uast).each {
            foundCheckForError = true
            def contextualNode = visitor.getContextualNode(it.underlyingNode)
            def cyclomaticComplexity = contextualNode.getAttributes().get("cyclomatic_complexity")
            assertEquals(4, cyclomaticComplexity)
        }
        MultiFilter.matchAll(new FunctionFilter(), new NameFilter("isReady"))
                .getFilteredNodes(language, processedFile.parseResponse.uast).each {
            foundIsReady = true
            def contextualNode = visitor.getContextualNode(it.underlyingNode)
            def cyclomaticComplexity = contextualNode.getAttributes().get("cyclomatic_complexity")
            assertEquals(1, cyclomaticComplexity)
        }
        assertTrue(foundCheckForError)
        assertTrue(foundIsReady)
    }

    @Test
    void javaCyclomaticComplexity_withSave() {
        def file = new File(".", "/src/test/resources/java/CyclomaticComplexity.java")
        def language = SourceLanguage.getSourceLanguage(file)
        def phenomena = new Phenomena()
        phenomena.connectToBabelfish()
        phenomena.connectToGrakn()
        phenomena.setupVisitor(new CodeStructureObserver(), new CyclomaticComplexity())
        phenomena.setupOntology()
        def processedFile = phenomena.processSourceFile(file, language)
        assertNotNull(processedFile.rootNodeId)
        println processedFile.rootNodeId
    }
}