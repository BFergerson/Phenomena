package com.codebrig.phenomena.code

import com.codebrig.arthur.observe.structure.filter.FunctionFilter
import com.codebrig.phenomena.Phenomena
import com.codebrig.phenomena.code.structure.CodeStructureObserver
import grakn.client.GraknClient
import groovy.util.logging.Slf4j
import org.junit.Before
import org.junit.Test

import java.util.stream.Collectors

import static org.junit.Assert.assertTrue

@Slf4j
class CodeObserverVisitorTest {

    @Before
    void setupGrakn() {
        try (def graknClient = new GraknClient("localhost:1729")) {
            graknClient.databases().delete("grakn")
            graknClient.databases().create("grakn")
        }
    }

    @Test
    void onlyVisitFunctionDeclarations() {
        def phenomena = new Phenomena()
        phenomena.scanPath = new ArrayList<>()
        phenomena.scanPath.add(new File(".", "/src/test/resources/same").absolutePath)

        def visitor = new CodeObserverVisitor()
        visitor.addObserver(new CodeStructureObserver(new FunctionFilter()))
        phenomena.setupVisitor(visitor)
        phenomena.connectToBabelfish()
        phenomena.processScanPath().map({ it.rootNodeId }).collect(Collectors.toList())
        phenomena.close()

        assertTrue(visitor.getObservedContextualNodes().every { new FunctionFilter().evaluate(it) })
    }
}
