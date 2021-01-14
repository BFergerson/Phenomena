package com.codebrig.phenomena.code.analysis.language.java.dependence

import com.codebrig.arthur.SourceLanguage
import com.codebrig.arthur.SourceNode
import com.codebrig.arthur.observe.structure.filter.TypeFilter
import com.codebrig.phenomena.Phenomena
import com.codebrig.phenomena.code.CodeObserverVisitor
import com.codebrig.phenomena.code.ContextualNode
import com.codebrig.phenomena.code.analysis.language.java.JavaParserIntegration
import com.codebrig.phenomena.code.structure.CodeStructureObserver
import grakn.client.GraknClient
import groovy.util.logging.Slf4j
import org.junit.Before
import org.junit.Test

import java.util.stream.Collectors

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull

@Slf4j
class JavaMethodCallObserverTest {

    @Before
    void setupGrakn() {
        try (def graknClient = new GraknClient("localhost:1729")) {
            if (graknClient.databases().contains("grakn")) {
                graknClient.databases().delete("grakn")
            }
            graknClient.databases().create("grakn")
        }
    }

    @Test
    void simpleMethodCall_noSave() {
        def phenomena = new Phenomena()
        phenomena.scanPath = new ArrayList<>()
        phenomena.scanPath.add(new File(".", "/src/test/resources/java/CallMethod.java").absolutePath)

        def visitor = new CodeObserverVisitor()
        visitor.addObserver(new CodeStructureObserver())
        visitor.addObserver(new JavaMethodCallObserver(new JavaParserIntegration(phenomena)))
        phenomena.setupVisitor(visitor)
        phenomena.connectToBabelfish()

        def processedFile = phenomena.processScanPath().findAny().get()
        def sourceNode = new SourceNode(SourceLanguage.Java, processedFile.parseResponse.uast)
        new TypeFilter("MethodInvocation").getFilteredNodes(sourceNode).each {
            def contextualNode = visitor.getContextualNode(it.underlyingNode)
            assertNotNull(contextualNode)
            def calledMethod = contextualNode.relationships.get(new ContextualNode.NodeRelationship("method_call"))
            assertNotNull(calledMethod)
            assertEquals("CallMethod.b_method(int)", calledMethod.name)
        }
        phenomena.close()
    }

    @Test
    void simpleMethodCall_withSave() {
        def phenomena = new Phenomena()
        phenomena.scanPath = new ArrayList<>()
        phenomena.scanPath.add(new File(".", "/src/test/resources/java/CallMethod.java").absolutePath)
        phenomena.connectToBabelfish()
        phenomena.connectToGrakn()
        phenomena.setupVisitor(new CodeStructureObserver(), new JavaMethodCallObserver(new JavaParserIntegration(phenomena)))
        phenomena.setupOntology()
        log.info phenomena.processScanPath().map({ it.rootNodeId }).collect(Collectors.toList()).toListString()
        phenomena.close()
    }

    @Test
    void externalMethodCall_withSave() {
        def phenomena = new Phenomena()
        phenomena.scanPath = new ArrayList<>()
        phenomena.scanPath.add(new File(".", "/src/test/resources/java/CallExternalMethod.java").absolutePath)
        phenomena.connectToBabelfish()
        phenomena.connectToGrakn()
        phenomena.setupVisitor(new CodeStructureObserver(), new JavaMethodCallObserver(new JavaParserIntegration(phenomena)))
        phenomena.setupOntology()
        log.info phenomena.processScanPath().map({ it.rootNodeId }).collect(Collectors.toList()).toListString()
        phenomena.close()
    }
}