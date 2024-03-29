package com.codebrig.phenomena.code

import com.codebrig.arthur.SourceLanguage
import com.codebrig.arthur.SourceNode
import com.codebrig.arthur.observe.structure.filter.CompilationUnitFilter
import com.codebrig.arthur.observe.structure.filter.FunctionFilter
import com.codebrig.arthur.observe.structure.filter.MultiFilter
import com.codebrig.phenomena.Phenomena
import com.codebrig.phenomena.PhenomenaTest
import com.codebrig.phenomena.code.structure.CodeStructureObserver
import com.vaticle.typedb.client.api.connection.TypeDBSession
import com.vaticle.typedb.client.api.connection.TypeDBTransaction
import org.junit.Test

import java.util.stream.Collectors

import static org.junit.Assert.*

class ContextualNodeTest extends PhenomenaTest {

    @Test
    void "double save node"() {
        def sourceFile = new File(".", "/src/test/resources/java/InnerMethodIdentifier.java")
        def phenomena = new Phenomena()
        phenomena.scanPath = new ArrayList<>()
        phenomena.scanPath.add(sourceFile.absolutePath)
        def visitor = new CodeObserverVisitor()
        visitor.addObserver(new CodeStructureObserver())
        phenomena.setupVisitor(visitor)
        phenomena.connectToBabelfish()
        phenomena.connectToGrakn()
        phenomena.setupOntology()

        def processedFile = phenomena.processScanPath().findAny().get()
        def sourceNode = new SourceNode(SourceLanguage.Java, processedFile.parseResponse.uast)
        def contextualNode = visitor.getOrCreateContextualNode(sourceNode, sourceFile)

        def session = phenomena.graknClient.session(phenomena.graknKeyspace, TypeDBSession.Type.DATA) //phenomena.getDataSession()
        def tx = session.transaction(TypeDBTransaction.Type.WRITE)
        contextualNode.save(tx)
        contextualNode.save(tx)
        tx.commit()
        tx.close()
        session.close()
        phenomena.close()
    }

    @Test
    void filteredContextualNodeParent() {
        def phenomena = new Phenomena()
        phenomena.scanPath = new ArrayList<>()
        phenomena.scanPath.add(new File(".", "/src/test/resources/same").absolutePath)

        def compilationUnitFilter = new CompilationUnitFilter()
        def functionDeclarationFilter = new FunctionFilter()
        def compilationOrFunctionFilter = MultiFilter.matchAny(
                compilationUnitFilter, functionDeclarationFilter
        )
        def visitor = new CodeObserverVisitor()
        visitor.addObserver(new CodeStructureObserver(compilationOrFunctionFilter))
        phenomena.setupVisitor(visitor)
        phenomena.connectToBabelfish()
        phenomena.processScanPath().map({ it.rootNodeId }).collect(Collectors.toList())
        phenomena.close()

        def observedNodes = visitor.getObservedContextualNodes()
        assertFalse(observedNodes.isEmpty())
        assertTrue(observedNodes.every { compilationOrFunctionFilter.evaluate(it) })
        assertTrue(observedNodes.any { compilationUnitFilter.evaluate(it) })
        assertTrue(observedNodes.any { functionDeclarationFilter.evaluate(it) })

        functionDeclarationFilter.getFilteredNodes(observedNodes).each { ContextualNode it ->
            assertTrue(compilationUnitFilter.evaluate(it.parentSourceNode))
            assertNull(it.parentSourceNode.parentSourceNode)
        }
    }
}