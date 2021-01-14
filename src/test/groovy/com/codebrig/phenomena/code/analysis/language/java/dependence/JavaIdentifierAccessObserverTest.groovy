package com.codebrig.phenomena.code.analysis.language.java.dependence

import com.codebrig.arthur.SourceLanguage
import com.codebrig.arthur.SourceNode
import com.codebrig.arthur.observe.structure.filter.MultiFilter
import com.codebrig.arthur.observe.structure.filter.NameFilter
import com.codebrig.arthur.observe.structure.filter.RoleFilter
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
class JavaIdentifierAccessObserverTest {

    @Before
    void setupGrakn() {
        try (def graknClient = new GraknClient("localhost:1729")) {
            graknClient.databases().delete("grakn")
            graknClient.databases().create("grakn")
        }
    }

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

        def arrayListDeclaration = new NameFilter("arrayList").getFilteredNodes(sourceNode)[0]
        def yayDeclaration = new NameFilter("yay").getFilteredNodes(sourceNode)[0]
        assertNotNull(arrayListDeclaration)
        assertNotNull(yayDeclaration)

        def arrayListUsage = new NameFilter("arrayList").getFilteredNodes(sourceNode)[1]
        def yayUsage = new NameFilter("yay").getFilteredNodes(sourceNode)[1]
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

        def simpleNameFilter = new TypeFilter("SimpleName")
        def xArg = MultiFilter.matchAll(simpleNameFilter, new NameFilter("x")).getFilteredNodes(sourceNode)[0]
        def yArg = MultiFilter.matchAll(simpleNameFilter, new NameFilter("y")).getFilteredNodes(sourceNode)[0]
        assertNotNull(xArg)
        assertNotNull(yArg)

        def notNameFilter = new RoleFilter().reject("NAME")
        def xVar = MultiFilter.matchAll(simpleNameFilter, notNameFilter, new NameFilter("x")).getFilteredNodes(sourceNode)[0]
        def yVar = MultiFilter.matchAll(simpleNameFilter, notNameFilter, new NameFilter("y")).getFilteredNodes(sourceNode)[0]
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
        log.info phenomena.processScanPath().map({ it.rootNodeId }).collect(Collectors.toList()).toListString()
        phenomena.close()
    }
}