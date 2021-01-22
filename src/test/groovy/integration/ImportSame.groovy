package integration

import com.codebrig.arthur.observe.structure.filter.MultiFilter
import com.codebrig.arthur.observe.structure.filter.RoleFilter
import com.codebrig.arthur.observe.structure.filter.TypeFilter
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
class ImportSame {

    @Before
    void setupGrakn() {
        try (def graknClient = new GraknClient("172.19.0.1:1729")) {
            if (graknClient.databases().contains("grakn")) {
                graknClient.databases().delete("grakn")
            }
            graknClient.databases().create("grakn")
        }
    }

    @Test
    void importSame_fullSchema() {
        def phenomena = new Phenomena()
        phenomena.scanPath = new ArrayList<>()
        phenomena.scanPath.add(new File(".", "/src/test/resources/same").absolutePath)
        phenomena.init()
        phenomena.setupOntology()
        log.info phenomena.processScanPath().map({ it.rootNodeId }).collect(Collectors.toList()).toListString()
        phenomena.close()
    }

    @Test
    void importSame_necessarySchema() {
        def phenomena = new Phenomena()
        phenomena.scanPath = new ArrayList<>()
        phenomena.scanPath.add(new File(".", "/src/test/resources/same").absolutePath)

        def multiFilter = new MultiFilter(MultiFilter.MatchStyle.ANY)
        def roleFilter = new RoleFilter("FILE", "DECLARATION_FUNCTION")
        multiFilter.accept(roleFilter)
        multiFilter.accept(new TypeFilter("MethodDeclaration"))
        phenomena.init(new CodeStructureObserver(multiFilter))
        phenomena.setupOntology()
        log.info phenomena.processScanPath().map({ it.rootNodeId }).collect(Collectors.toList()).toListString()
        phenomena.close()
    }

    @Test
    void importSame_withAllObservers() {
        def phenomena = new Phenomena()
        phenomena.scanPath = new ArrayList<>()
        phenomena.scanPath.add(new File(".", "/src/test/resources/same").absolutePath)
        def observers = new ArrayList<CodeObserver>()
        observers.add(new CodeStructureObserver())
        observers.addAll(DependenceAnalysis.getAllCodeObservers(phenomena))
        observers.addAll(MetricAnalysis.getAllCodeObservers(phenomena))
        phenomena.init(observers)
        phenomena.setupOntology()
        log.info phenomena.processScanPath().map({ it.rootNodeId }).collect(Collectors.toList()).toListString()
        phenomena.close()
    }
}
