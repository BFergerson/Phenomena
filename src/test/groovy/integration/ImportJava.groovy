package integration

import com.codebrig.phenomena.Phenomena
import org.junit.Test

import java.util.stream.Collectors

class ImportJava {

    @Test
    void doThing() {
        def phenomena = new Phenomena()
        phenomena.scanPath = new ArrayList<>()
        phenomena.scanPath.add(new File(".", "/src/test/resources/java").absolutePath)
        phenomena.init()
        phenomena.setupOntology()
        def list = phenomena.processScanPath()
        println list.stream().map({ it.rootNodeId }).collect(Collectors.toList()).toListString()
    }
}
