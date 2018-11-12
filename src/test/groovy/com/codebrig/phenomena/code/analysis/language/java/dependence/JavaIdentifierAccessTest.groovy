package com.codebrig.phenomena.code.analysis.language.java.dependence

import com.codebrig.omnisrc.SourceLanguage
import com.codebrig.omnisrc.SourceNode
import com.codebrig.omnisrc.schema.filter.BlacklistRoleFilter
import com.codebrig.omnisrc.schema.filter.MultiFilter
import com.codebrig.omnisrc.schema.filter.NameFilter
import com.codebrig.omnisrc.schema.filter.SimpleNameFilter
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

class JavaIdentifierAccessTest extends PhenomenaTest {

    @Test
    void innerMethodIdentifierAccess_noSave() {
        def phenomena = new Phenomena()
        phenomena.scanPath = new ArrayList<>()
        phenomena.scanPath.add(new File(".", "/src/test/resources/java/InnerMethodIdentifier.java").absolutePath)

        def visitor = new CodeObserverVisitor()
        visitor.addObserver(new CodeStructureObserver())
        visitor.addObserver(new JavaIdentifierAccess(new JavaParserIntegration(phenomena)))
        phenomena.setupVisitor(visitor)
        phenomena.connectToBabelfish()

        def processedFile = phenomena.processScanPath().findAny().get()
        def sourceNode = new SourceNode(SourceLanguage.Java, processedFile.parseResponse.uast)
        def arrayListArg = new NameFilter("arrayList").getFilteredNodes(sourceNode)[0]
        def yayArg = new NameFilter("yay").getFilteredNodes(sourceNode)[0]
        assertNotNull(arrayListArg)
        assertNotNull(yayArg)

        def arrayListVar = new SimpleNameFilter("arrayList").getFilteredNodes(sourceNode)[1]
        def yayVar = new SimpleNameFilter("yay").getFilteredNodes(sourceNode)[1]
        assertNotNull(arrayListVar)
        assertNotNull(yayVar)

        def contextualArrayList = visitor.getContextualNode(arrayListVar.underlyingNode)
        def contextualYay = visitor.getContextualNode(yayVar.underlyingNode)
        assertNotNull(contextualArrayList)
        assertNotNull(contextualYay)

        def arrayListAccessTo = contextualArrayList.relationships.get(new ContextualNode.NodeRelationship("identifier_access"))
        def yayAccessTo = contextualYay.relationships.get(new ContextualNode.NodeRelationship("identifier_access"))
        assertEquals(arrayListArg.underlyingNode, arrayListAccessTo.underlyingNode)
        assertEquals(yayArg.underlyingNode, yayAccessTo.underlyingNode)
    }

    @Test
    void simpleIdentifierAccess_noSave() {
        def phenomena = new Phenomena()
        phenomena.scanPath = new ArrayList<>()
        phenomena.scanPath.add(new File(".", "/src/test/resources/java/CallMethod.java").absolutePath)

        def visitor = new CodeObserverVisitor()
        visitor.addObserver(new CodeStructureObserver())
        visitor.addObserver(new JavaIdentifierAccess(new JavaParserIntegration(phenomena)))
        phenomena.setupVisitor(visitor)
        phenomena.connectToBabelfish()

        def processedFile = phenomena.processScanPath().findAny().get()
        def sourceNode = new SourceNode(SourceLanguage.Java, processedFile.parseResponse.uast)
        def xArg = new NameFilter("x").getFilteredNodes(sourceNode)[0]
        def yArg = new NameFilter("y").getFilteredNodes(sourceNode)[0]
        assertNotNull(xArg)
        assertNotNull(yArg)

        def notNameFilter = new BlacklistRoleFilter()
        notNameFilter.rejectRole("NAME")
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
    }

    @Test
    void simpleIdentifierAccess_withSave() {
        def phenomena = new Phenomena()
        phenomena.scanPath = new ArrayList<>()
        phenomena.scanPath.add(new File(".", "/src/test/resources/java/CallMethod.java").absolutePath)
        phenomena.connectToBabelfish()
        phenomena.connectToGrakn()
        phenomena.setupVisitor(new CodeStructureObserver(), new JavaIdentifierAccess(new JavaParserIntegration(phenomena)))
        phenomena.setupOntology()
        println phenomena.processScanPath().map({ it.rootNodeId }).collect(Collectors.toList()).toListString()
    }
}