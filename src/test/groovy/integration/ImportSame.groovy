package integration

import com.codebrig.omnisrc.observe.filter.MultiFilter
import com.codebrig.omnisrc.observe.filter.RoleFilter
import com.codebrig.omnisrc.observe.filter.TypeFilter
import com.codebrig.phenomena.Phenomena
import com.codebrig.phenomena.code.CodeObserver
import com.codebrig.phenomena.code.analysis.DependenceAnalysis
import com.codebrig.phenomena.code.analysis.MetricAnalysis
import com.codebrig.phenomena.code.structure.CodeStructureObserver
import org.junit.Test

import java.util.stream.Collectors

class ImportSame {

    @Test
    void importSame_fullSchema() {
        def phenomena = new Phenomena()
        phenomena.setGraknKeyspace("full_schema")
        phenomena.scanPath = new ArrayList<>()
        phenomena.scanPath.add(new File(".", "/src/test/resources/same").absolutePath)
        phenomena.init()
        phenomena.setupOntology()
        println phenomena.processScanPath().map({ it.rootNodeId }).collect(Collectors.toList()).toListString()
        phenomena.close()
    }

    @Test
    void importSame_necessarySchema() {
        def phenomena = new Phenomena()
        phenomena.setGraknKeyspace("necessary_schema")
        phenomena.scanPath = new ArrayList<>()
        phenomena.scanPath.add(new File(".", "/src/test/resources/same").absolutePath)

        def multiFilter = new MultiFilter(MultiFilter.MatchStyle.ANY)
        def roleFilter = new RoleFilter("FILE", "DECLARATION_FUNCTION")
        multiFilter.accept(roleFilter)
        multiFilter.accept(new TypeFilter("MethodDeclaration"))
        phenomena.init(new CodeStructureObserver(multiFilter))
        phenomena.setupOntology(new File(".", "/src/test/resources/schema/Same_Schema.gql").text)
        println phenomena.processScanPath().map({ it.rootNodeId }).collect(Collectors.toList()).toListString()
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
        println phenomena.processScanPath().map({ it.rootNodeId }).collect(Collectors.toList()).toListString()
        phenomena.close()
    }
}
