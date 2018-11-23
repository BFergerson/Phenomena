package integration

import com.codebrig.phenomena.Phenomena
import org.junit.Test

import java.util.stream.Collectors

class ImportJava {

    @Test
    void importAllJava() {
        def phenomena = new Phenomena()
        phenomena.scanPath = new ArrayList<>()
        phenomena.scanPath.add(new File(".", "/src/test/resources/java").absolutePath)
        phenomena.init()
        phenomena.setupOntology()
        println phenomena.processScanPath().map({ it.rootNodeId }).collect(Collectors.toList()).toListString()
        phenomena.close()
    }
}
