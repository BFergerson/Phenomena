package integration

import com.codebrig.omnisrc.schema.filter.MultiFilter
import com.codebrig.omnisrc.schema.filter.TypeFilter
import com.codebrig.omnisrc.schema.filter.WhitelistRoleFilter
import com.codebrig.phenomena.Phenomena
import com.codebrig.phenomena.code.structure.CodeStructureObserver
import org.junit.Test

import java.util.stream.Collectors

class ImportSame {

    @Test
    void importSame_fullSchema() {
        def phenomena = new Phenomena()
        phenomena.scanPath = new ArrayList<>()
        phenomena.scanPath.add(new File(".", "/src/test/resources/same").absolutePath)
        phenomena.init()
        phenomena.setupOntology()
        println phenomena.processScanPath().map({ it.rootNodeId }).collect(Collectors.toList()).toListString()
    }

    @Test
    void importSame_necessarySchema() {
        def phenomena = new Phenomena()
        phenomena.scanPath = new ArrayList<>()
        phenomena.scanPath.add(new File(".", "/src/test/resources/same").absolutePath)

        def multiFilter = new MultiFilter()
        def roleFilter = new WhitelistRoleFilter("FILE", "DECLARATION_FUNCTION")
        multiFilter.acceptFilter(roleFilter)
        multiFilter.acceptFilter(new TypeFilter("MethodDeclaration"))
        phenomena.init(new CodeStructureObserver(multiFilter))
        phenomena.setupOntology(new File(".", "/src/test/resources/schema/Same_Schema.gql").text)
        println phenomena.processScanPath().map({ it.rootNodeId }).collect(Collectors.toList()).toListString()
    }
}
