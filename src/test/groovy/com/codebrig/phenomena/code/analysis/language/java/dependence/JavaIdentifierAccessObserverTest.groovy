package com.codebrig.phenomena.code.analysis.language.java.dependence

import com.codebrig.omnisrc.SourceLanguage
import com.codebrig.omnisrc.SourceNode
import com.codebrig.omnisrc.observe.filter.MultiFilter
import com.codebrig.omnisrc.observe.filter.NameFilter
import com.codebrig.omnisrc.observe.filter.RoleFilter
import com.codebrig.omnisrc.observe.filter.SimpleNameFilter
import com.codebrig.phenomena.Phenomena
import com.codebrig.phenomena.PhenomenaTest
import com.codebrig.phenomena.code.CodeObserverVisitor
import com.codebrig.phenomena.code.ContextualNode
import com.codebrig.phenomena.code.analysis.language.java.JavaParserIntegration
import com.codebrig.phenomena.code.structure.CodeStructureObserver
import org.junit.Test

import java.util.stream.Collectors

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull

class JavaIdentifierAccessObserverTest extends PhenomenaTest {

    @Test
    void innerMethodIdentifierAccess_noSave() {
        def phenomena = new Phenomena()
        phenomena.scanPath = new ArrayList<>()
        phenomena.scanPath.add(new File(".", "/src/test/resources/java/InnerMethodIdentifier.java").absolutePath)

        def visitor = new CodeObserverVisitor()
        visitor.addObserver(new CodeStructureObserver())
        visitor.addObserver(new JavaIdentifierAccessObserver(new JavaParserIntegration(phenomena)))
        phenomena.setupVisitor(visitor)
        phenomena.connectToBabelfish()
        def processedFile = phenomena.processScanPath().findAny().get()
        def sourceNode = new SourceNode(SourceLanguage.Java, processedFile.parseResponse.uast)

        def arrayListDeclaration = new SimpleNameFilter("arrayList").getFilteredNodes(sourceNode)[0]
        def yayDeclaration = new SimpleNameFilter("yay").getFilteredNodes(sourceNode)[0]
        assertNotNull(arrayListDeclaration)
        assertNotNull(yayDeclaration)

        def arrayListUsage = new SimpleNameFilter("arrayList").getFilteredNodes(sourceNode)[1]
        def yayUsage = new SimpleNameFilter("yay").getFilteredNodes(sourceNode)[1]
        assertNotNull(arrayListUsage)
        assertNotNull(yayUsage)

        def contextualArrayListUsage = visitor.getContextualNode(arrayListUsage.underlyingNode)
        def contextualYayUsage = visitor.getContextualNode(yayUsage.underlyingNode)
        assertNotNull(contextualArrayListUsage)
        assertNotNull(contextualYayUsage)

        def arrayListAccessTo = contextualArrayListUsage.relationships.get(
                new ContextualNode.NodeRelationship("identifier_access"))
        def yayAccessTo = contextualYayUsage.relationships.get(
                new ContextualNode.NodeRelationship("identifier_access"))
        assertEquals(arrayListDeclaration.underlyingNode, arrayListAccessTo.underlyingNode)
        assertEquals(yayDeclaration.underlyingNode, yayAccessTo.underlyingNode)
        phenomena.close()
    }

    @Test
    void simpleIdentifierAccess_noSave() {
        def phenomena = new Phenomena()
        phenomena.scanPath = new ArrayList<>()
        phenomena.scanPath.add(new File(".", "/src/test/resources/java/CallMethod.java").absolutePath)

        def visitor = new CodeObserverVisitor()
        visitor.addObserver(new CodeStructureObserver())
        visitor.addObserver(new JavaIdentifierAccessObserver(new JavaParserIntegration(phenomena)))
        phenomena.setupVisitor(visitor)
        phenomena.connectToBabelfish()
        def processedFile = phenomena.processScanPath().findAny().get()
        def sourceNode = new SourceNode(SourceLanguage.Java, processedFile.parseResponse.uast)

        def xArg = new SimpleNameFilter("x").getFilteredNodes(sourceNode)[0]
        def yArg = new SimpleNameFilter("y").getFilteredNodes(sourceNode)[0]
        assertNotNull(xArg)
        assertNotNull(yArg)

        def notNameFilter = new RoleFilter().reject("NAME")
        def xVar = MultiFilter.matchAll(notNameFilter, new SimpleNameFilter("x")).getFilteredNodes(sourceNode)[0]
        def yVar = MultiFilter.matchAll(notNameFilter, new SimpleNameFilter("y")).getFilteredNodes(sourceNode)[0]
        assertNotNull(xVar)
        assertNotNull(yVar)

        def contextualX = visitor.getContextualNode(xVar.underlyingNode)
        def contextualY = visitor.getContextualNode(yVar.underlyingNode)
        assertNotNull(contextualX)
        assertNotNull(contextualY)

        def xAccessTo = contextualX.relationships.get(new ContextualNode.NodeRelationship("identifier_access"))
        def yAccessTo = contextualY.relationships.get(new ContextualNode.NodeRelationship("identifier_access"))
        assertEquals(xArg.underlyingNode, xAccessTo.underlyingNode)
        assertEquals(yArg.underlyingNode, yAccessTo.underlyingNode)
        phenomena.close()
    }

    @Test
    void simpleIdentifierAccess_withSave() {
        def phenomena = new Phenomena()
        phenomena.scanPath = new ArrayList<>()
        phenomena.scanPath.add(new File(".", "/src/test/resources/java/CallMethod.java").absolutePath)
        phenomena.connectToBabelfish()
        phenomena.connectToGrakn()
        phenomena.setupVisitor(new CodeStructureObserver(), new JavaIdentifierAccessObserver(new JavaParserIntegration(phenomena)))
        phenomena.setupOntology()
        println phenomena.processScanPath().map({ it.rootNodeId }).collect(Collectors.toList()).toListString()
        phenomena.close()
    }
}