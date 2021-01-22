package com.codebrig.phenomena.code.analysis.metric

import com.codebrig.arthur.SourceLanguage
import com.codebrig.arthur.observe.structure.filter.FunctionFilter
import com.codebrig.arthur.observe.structure.filter.MultiFilter
import com.codebrig.arthur.observe.structure.filter.NameFilter
import com.codebrig.phenomena.Phenomena
import com.codebrig.phenomena.PhenomenaTest
import com.codebrig.phenomena.code.CodeObserverVisitor
import com.codebrig.phenomena.code.structure.CodeStructureObserver
import groovy.util.logging.Slf4j
import org.junit.Test

import static org.junit.Assert.*

@Slf4j
class CyclomaticComplexityObserverTest extends PhenomenaTest {

    @Test
    void pythonInnerMethod_noSave() {
        def file = new File(".", "/src/test/resources/python/InnerMethodComplexity.py")
        def language = SourceLanguage.getSourceLanguage(file)
        def phenomena = new Phenomena()
        def visitor = new CodeObserverVisitor()
        visitor.addObserver(new CyclomaticComplexityObserver())
        phenomena.setupVisitor(visitor)
        phenomena.connectToBabelfish()
        def processedFile = phenomena.processSourceFile(file, language)

        def foundOuter = false
        def foundInner = false
        MultiFilter.matchAll(new FunctionFilter(), new NameFilter("outer"))
                .getFilteredNodes(language, processedFile.parseResponse.uast).each {
            foundOuter = true
            def contextualNode = visitor.getContextualNode(it.underlyingNode)
            def cyclomaticComplexity = contextualNode.getAttributes().get("cyclomaticComplexity")
            assertEquals(1, cyclomaticComplexity)
        }
        MultiFilter.matchAll(new FunctionFilter(), new NameFilter("inner_increment"))
                .getFilteredNodes(language, processedFile.parseResponse.uast).each {
            foundInner = true
            def contextualNode = visitor.getContextualNode(it.underlyingNode)
            def cyclomaticComplexity = contextualNode.getAttributes().get("cyclomaticComplexity")
            assertEquals(3, cyclomaticComplexity)
        }
        assertTrue(foundOuter)
        assertTrue(foundInner)
        phenomena.close()
    }

    @Test
    void pythonInnerMethod_withSave() {
        def file = new File(".", "/src/test/resources/python/InnerMethodComplexity.py")
        def language = SourceLanguage.getSourceLanguage(file)
        def phenomena = new Phenomena()
        phenomena.connectToBabelfish()
        phenomena.connectToGrakn()
        phenomena.setupVisitor(new CodeStructureObserver(), new CyclomaticComplexityObserver())
        phenomena.setupOntology()
        def processedFile = phenomena.processSourceFile(file, language)
        assertNotNull(processedFile.rootNodeId)
        log.info processedFile.rootNodeId
        phenomena.close()
    }

    @Test
    void javaCyclomaticComplexity_noSave() {
        def file = new File(".", "/src/test/resources/java/CyclomaticComplexity.java")
        def language = SourceLanguage.getSourceLanguage(file)
        def phenomena = new Phenomena()
        def visitor = new CodeObserverVisitor()
        visitor.addObserver(new CyclomaticComplexityObserver())
        phenomena.setupVisitor(visitor)
        phenomena.connectToBabelfish()
        def processedFile = phenomena.processSourceFile(file, language)

        def foundCheckForError = false
        def foundIsReady = false
        //todo: should be able to do name filter without using qualified name
        MultiFilter.matchAll(new FunctionFilter(), new NameFilter("CyclomaticComplexity.checkForError"))
                .getFilteredNodes(language, processedFile.parseResponse.uast).each {
            foundCheckForError = true
            def contextualNode = visitor.getContextualNode(it.underlyingNode)
            def cyclomaticComplexity = contextualNode.getAttributes().get("cyclomaticComplexity")
            assertEquals(4, cyclomaticComplexity)
        }
        MultiFilter.matchAll(new FunctionFilter(), new NameFilter("CyclomaticComplexity.isReady"))
                .getFilteredNodes(language, processedFile.parseResponse.uast).each {
            foundIsReady = true
            def contextualNode = visitor.getContextualNode(it.underlyingNode)
            def cyclomaticComplexity = contextualNode.getAttributes().get("cyclomaticComplexity")
            assertEquals(1, cyclomaticComplexity)
        }
        assertTrue(foundCheckForError)
        assertTrue(foundIsReady)
        phenomena.close()
    }

    @Test
    void javaCyclomaticComplexity_withSave() {
        def file = new File(".", "/src/test/resources/java/CyclomaticComplexity.java")
        def language = SourceLanguage.getSourceLanguage(file)
        def phenomena = new Phenomena()
        phenomena.connectToBabelfish()
        phenomena.connectToGrakn()
        phenomena.setupVisitor(new CodeStructureObserver(), new CyclomaticComplexityObserver())
        phenomena.setupOntology()
        def processedFile = phenomena.processSourceFile(file, language)
        assertNotNull(processedFile.rootNodeId)
        log.info processedFile.rootNodeId
        phenomena.close()
    }

    @Test
    void goCyclomaticComplexity_noSave() {
        def file = new File(".", "/src/test/resources/go/CyclomaticComplexity.go")
        def language = SourceLanguage.getSourceLanguage(file)
        def phenomena = new Phenomena()
        def visitor = new CodeObserverVisitor()
        visitor.addObserver(new CyclomaticComplexityObserver())
        phenomena.setupVisitor(visitor)
        phenomena.connectToBabelfish()
        def processedFile = phenomena.processSourceFile(file, language)

        def foundSortaComplex = false
        MultiFilter.matchAll(new FunctionFilter(), new NameFilter("sortaComplex"))
                .getFilteredNodes(language, processedFile.parseResponse.uast).each {
            foundSortaComplex = true
            def contextualNode = visitor.getContextualNode(it.underlyingNode)
            def cyclomaticComplexity = contextualNode.getAttributes().get("cyclomaticComplexity")
            assertEquals(7, cyclomaticComplexity)
        }
        assertTrue(foundSortaComplex)
        phenomena.close()
    }

    @Test
    void goCyclomaticComplexity_withSave() {
        def file = new File(".", "/src/test/resources/go/CyclomaticComplexity.go")
        def language = SourceLanguage.getSourceLanguage(file)
        def phenomena = new Phenomena()
        phenomena.connectToBabelfish()
        phenomena.connectToGrakn()
        phenomena.setupVisitor(new CodeStructureObserver(), new CyclomaticComplexityObserver())
        phenomena.setupOntology()
        def processedFile = phenomena.processSourceFile(file, language)
        assertNotNull(processedFile.rootNodeId)
        log.info processedFile.rootNodeId
        phenomena.close()
    }

    @Test
    void javascriptCyclomaticComplexity_noSave() {
        def file = new File(".", "/src/test/resources/javascript/CyclomaticComplexity.js")
        def language = SourceLanguage.getSourceLanguage(file)
        def phenomena = new Phenomena()
        def visitor = new CodeObserverVisitor()
        visitor.addObserver(new CyclomaticComplexityObserver())
        phenomena.setupVisitor(visitor)
        phenomena.connectToBabelfish()
        def processedFile = phenomena.processSourceFile(file, language)

        def foundSortaComplex = false
        MultiFilter.matchAll(new FunctionFilter(), new NameFilter("sortaComplex"))
                .getFilteredNodes(language, processedFile.parseResponse.uast).each {
            foundSortaComplex = true
            def contextualNode = visitor.getContextualNode(it.underlyingNode)
            def cyclomaticComplexity = contextualNode.getAttributes().get("cyclomaticComplexity")
            assertEquals(7, cyclomaticComplexity)
        }
        assertTrue(foundSortaComplex)
        phenomena.close()
    }

    @Test
    void javascriptCyclomaticComplexity_withSave() {
        def file = new File(".", "/src/test/resources/javascript/CyclomaticComplexity.js")
        def language = SourceLanguage.getSourceLanguage(file)
        def phenomena = new Phenomena()
        phenomena.connectToBabelfish()
        phenomena.connectToGrakn()
        phenomena.setupVisitor(new CodeStructureObserver(), new CyclomaticComplexityObserver())
        phenomena.setupOntology()
        def processedFile = phenomena.processSourceFile(file, language)
        assertNotNull(processedFile.rootNodeId)
        log.info processedFile.rootNodeId
        phenomena.close()
    }

    @Test
    void csharpCyclomaticComplexity_noSave() {
        def file = new File(".", "/src/test/resources/csharp/CyclomaticComplexity.cs")
        def language = SourceLanguage.getSourceLanguage(file)
        def phenomena = new Phenomena()
        def visitor = new CodeObserverVisitor()
        visitor.addObserver(new CyclomaticComplexityObserver())
        phenomena.setupVisitor(visitor)
        phenomena.connectToBabelfish()
        def processedFile = phenomena.processSourceFile(file, language)

        def foundSortaComplex = false
        MultiFilter.matchAll(new FunctionFilter(), new NameFilter("sortaComplex"))
                .getFilteredNodes(language, processedFile.parseResponse.uast).each {
            foundSortaComplex = true
            def contextualNode = visitor.getContextualNode(it.underlyingNode)
            def cyclomaticComplexity = contextualNode.getAttributes().get("cyclomaticComplexity")
            assertEquals(7, cyclomaticComplexity)
        }
        assertTrue(foundSortaComplex)
        phenomena.close()
    }

    @Test
    void csharpCyclomaticComplexity_withSave() {
        def file = new File(".", "/src/test/resources/csharp/CyclomaticComplexity.cs")
        def language = SourceLanguage.getSourceLanguage(file)
        def phenomena = new Phenomena()
        phenomena.connectToBabelfish()
        phenomena.connectToGrakn()
        phenomena.setupVisitor(new CodeStructureObserver(), new CyclomaticComplexityObserver())
        phenomena.setupOntology()
        def processedFile = phenomena.processSourceFile(file, language)
        assertNotNull(processedFile.rootNodeId)
        log.info processedFile.rootNodeId
        phenomena.close()
    }

    @Test
    void bashCyclomaticComplexity_noSave() {
        def file = new File(".", "/src/test/resources/bash/CyclomaticComplexity.sh")
        def language = SourceLanguage.getSourceLanguage(file)
        def phenomena = new Phenomena()
        def visitor = new CodeObserverVisitor()
        visitor.addObserver(new CyclomaticComplexityObserver())
        phenomena.setupVisitor(visitor)
        phenomena.connectToBabelfish()
        def processedFile = phenomena.processSourceFile(file, language)

        def foundSortaComplex = false
        MultiFilter.matchAll(new FunctionFilter(), new NameFilter("sortaComplex"))
                .getFilteredNodes(language, processedFile.parseResponse.uast).each {
            foundSortaComplex = true
            def contextualNode = visitor.getContextualNode(it.underlyingNode)
            def cyclomaticComplexity = contextualNode.getAttributes().get("cyclomaticComplexity")
            assertEquals(7, cyclomaticComplexity)
        }
        assertTrue(foundSortaComplex)
        phenomena.close()
    }

    @Test
    void bashCyclomaticComplexity_withSave() {
        def file = new File(".", "/src/test/resources/bash/CyclomaticComplexity.sh")
        def language = SourceLanguage.getSourceLanguage(file)
        def phenomena = new Phenomena()
        phenomena.connectToBabelfish()
        phenomena.connectToGrakn()
        phenomena.setupVisitor(new CodeStructureObserver(), new CyclomaticComplexityObserver())
        phenomena.setupOntology()
        def processedFile = phenomena.processSourceFile(file, language)
        assertNotNull(processedFile.rootNodeId)
        log.info processedFile.rootNodeId
        phenomena.close()
    }

    @Test
    void cplusplusCyclomaticComplexity_noSave() {
        def file = new File(".", "/src/test/resources/cplusplus/CyclomaticComplexity.cpp")
        def language = SourceLanguage.getSourceLanguage(file)
        def phenomena = new Phenomena()
        def visitor = new CodeObserverVisitor()
        visitor.addObserver(new CyclomaticComplexityObserver())
        phenomena.setupVisitor(visitor)
        phenomena.connectToBabelfish()
        def processedFile = phenomena.processSourceFile(file, language)

        def foundSortaComplex = false
        MultiFilter.matchAll(new FunctionFilter(), new NameFilter("sortaComplex"))
                .getFilteredNodes(language, processedFile.parseResponse.uast).each {
            foundSortaComplex = true
            def contextualNode = visitor.getContextualNode(it.underlyingNode)
            def cyclomaticComplexity = contextualNode.getAttributes().get("cyclomaticComplexity")
            assertEquals(7, cyclomaticComplexity)
        }
        assertTrue(foundSortaComplex)
        phenomena.close()
    }

    @Test
    void cplusplusCyclomaticComplexity_withSave() {
        def file = new File(".", "/src/test/resources/cplusplus/CyclomaticComplexity.cpp")
        def language = SourceLanguage.getSourceLanguage(file)
        def phenomena = new Phenomena()
        phenomena.connectToBabelfish()
        phenomena.connectToGrakn()
        phenomena.setupVisitor(new CodeStructureObserver(), new CyclomaticComplexityObserver())
        phenomena.setupOntology()
        def processedFile = phenomena.processSourceFile(file, language)
        assertNotNull(processedFile.rootNodeId)
        log.info processedFile.rootNodeId
        phenomena.close()
    }
}