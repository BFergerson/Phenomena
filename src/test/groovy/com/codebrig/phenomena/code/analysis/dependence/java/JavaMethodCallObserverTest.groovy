package com.codebrig.phenomena.code.analysis.dependence.java

import com.codebrig.omnisrc.SourceLanguage
import com.codebrig.omnisrc.SourceNode
import com.codebrig.omnisrc.schema.filter.TypeFilter
import com.codebrig.phenomena.Phenomena
import com.codebrig.phenomena.PhenomenaTest
import com.codebrig.phenomena.code.CodeObserverVisitor
import com.codebrig.phenomena.code.ContextualNode
import com.codebrig.phenomena.code.analysis.language.java.JavaParserIntegration
import com.codebrig.phenomena.code.analysis.language.java.dependence.JavaMethodCallObserver
import com.codebrig.phenomena.code.structure.CodeStructureObserver
import org.junit.Test

import java.util.stream.Collectors

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull

class JavaMethodCallObserverTest extends PhenomenaTest {

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
            assertEquals("CallMethod.b_method(int)", calledMethod.qualifiedName)
        }
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
        println phenomena.processScanPath().map({ it.rootNodeId }).collect(Collectors.toList()).toListString()
    }
}