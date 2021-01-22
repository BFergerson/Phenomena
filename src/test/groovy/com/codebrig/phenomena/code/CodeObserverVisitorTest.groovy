package com.codebrig.phenomena.code

import com.codebrig.arthur.observe.structure.filter.FunctionFilter
import com.codebrig.phenomena.Phenomena
import com.codebrig.phenomena.PhenomenaTest
import com.codebrig.phenomena.code.structure.CodeStructureObserver
import groovy.util.logging.Slf4j
import org.junit.Test

import java.util.stream.Collectors

import static org.junit.Assert.assertTrue

@Slf4j
class CodeObserverVisitorTest extends PhenomenaTest {

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
