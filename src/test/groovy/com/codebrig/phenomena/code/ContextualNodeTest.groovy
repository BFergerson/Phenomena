package com.codebrig.phenomena.code

import ai.grakn.GraknTxType
import com.codebrig.omnisrc.SourceLanguage
import com.codebrig.omnisrc.SourceNode
import com.codebrig.phenomena.Phenomena
import com.codebrig.phenomena.PhenomenaTest
import com.codebrig.phenomena.code.structure.CodeStructureObserver
import org.junit.Test

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

        def processedFile = phenomena.processScanPath().findAny().get()
        def sourceNode = new SourceNode(SourceLanguage.Java, processedFile.parseResponse.uast)
        def contextualNode = visitor.getOrCreateContextualNode(sourceNode, sourceFile)

        def session = phenomena.getGraknSession()
        def tx = session.transaction(GraknTxType.WRITE)
        def graql = tx.graql()
        contextualNode.save(graql)
        contextualNode.save(graql)
        tx.commit()
        tx.close()
        phenomena.close()
    }
}