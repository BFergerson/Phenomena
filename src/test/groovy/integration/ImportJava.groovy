package integration

import com.codebrig.arthur.SourceLanguage
import com.codebrig.phenomena.Phenomena
import com.codebrig.phenomena.code.CodeObserver
import com.codebrig.phenomena.code.analysis.DependenceAnalysis
import com.codebrig.phenomena.code.analysis.MetricAnalysis
import com.codebrig.phenomena.code.structure.CodeStructureObserver
import grakn.client.GraknClient
import groovy.util.logging.Slf4j
import org.junit.Before
import org.junit.Test

import java.util.stream.Collectors

@Slf4j
class ImportJava {

    @Before
    void setupGrakn() {
        try (def graknClient = GraknClient.core("localhost:1729")) {
            if (graknClient.databases().contains("grakn")) {
                graknClient.databases().delete("grakn")
            }
            graknClient.databases().create("grakn")
        }
    }

    @Test
    void importAllJava() {
        def phenomena = new Phenomena()
        phenomena.scanPath = new ArrayList<>()
        phenomena.scanPath.add(new File(".", "/src/test/resources/java").absolutePath)
        phenomena.init()
        phenomena.setupOntology()
        log.info phenomena.processScanPath().map({ it.rootNodeId }).collect(Collectors.toList()).toListString()
        phenomena.close()
    }

    @Test
    void importAllJava_withAllObservers() {
        def phenomena = new Phenomena()
        phenomena.scanPath = new ArrayList<>()
        phenomena.scanPath.add(new File(".", "/src/test/resources/java").absolutePath)
        def observers = new ArrayList<CodeObserver>()
        observers.add(new CodeStructureObserver())
        observers.addAll(DependenceAnalysis.getAllCodeObservers(phenomena, SourceLanguage.Java))
        observers.addAll(MetricAnalysis.getAllCodeObservers(phenomena, SourceLanguage.Java))
        phenomena.init(observers)
        phenomena.setupOntology()
        log.info phenomena.processScanPath().map({ it.rootNodeId }).collect(Collectors.toList()).toListString()
        phenomena.close()
    }
}
