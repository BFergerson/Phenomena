package com.codebrig.phenomena

import ai.grakn.GraknTxType
import ai.grakn.Keyspace
import ai.grakn.client.Grakn
import ai.grakn.util.SimpleURI
import com.codebrig.omnisrc.SourceLanguage
import com.codebrig.phenomena.code.CodeObserver
import com.codebrig.phenomena.code.CodeObserverVisitor
import com.codebrig.phenomena.code.ProcessedSourceFile
import com.codebrig.phenomena.code.structure.CodeStructureObserver
import com.google.common.collect.Streams
import gopkg.in.bblfsh.sdk.v1.protocol.generated.Encoding
import gopkg.in.bblfsh.sdk.v1.protocol.generated.ParseResponse
import org.apache.commons.collections4.Transformer
import org.apache.commons.collections4.iterators.TransformIterator
import org.bblfsh.client.BblfshClient

import java.util.stream.Stream

import static groovy.io.FileType.FILES

/**
 * Main entry-point used to parse, process, and store source code files
 *
 * @version 0.2
 * @since 0.1
 * @author <a href="mailto:brandon.fergerson@codebrig.com">Brandon Fergerson</a>
 */
class Phenomena {

    private static ResourceBundle buildBundle = ResourceBundle.getBundle("phenomena_build")
    public static final String PHENOMENA_VERSION = buildBundle.getString("version")
    private List<String> scanPath
    private List<String> activeObservers = ["structure"]
    private String graknHost = "localhost"
    private int graknPort = 48555
    private String graknKeyspace = "grakn"
    private String babelfishHost = "localhost"
    private int babelfishPort = 9432
    private CodeObserverVisitor visitor
    private BblfshClient parser
    private Grakn.Session session

    void init() {
        init(new CodeStructureObserver())
    }

    void init(List<CodeObserver> codeObservers) {
        Objects.requireNonNull(codeObservers)
        init(codeObservers.toArray(new CodeObserver[0]))
    }

    void init(CodeObserver... codeObservers) {
        if (codeObservers.length == 0) {
            throw new IllegalArgumentException("Missing code observers")
        }

        println "Initializing Phenomena (ver.$PHENOMENA_VERSION)"
        connectToBabelfish()
        connectToGrakn()
        setupVisitor(codeObservers)
    }

    void connectToBabelfish() {
        println "Connecting to Babelfish"
        parser = new BblfshClient(babelfishHost, babelfishPort, Integer.MAX_VALUE)
    }

    void connectToGrakn() {
        println "Connecting to Grakn"
        session = new Grakn(new SimpleURI(graknURI)).session(Keyspace.of(graknKeyspace))

        //test connection
        def testTx = session.transaction(GraknTxType.READ)
        testTx.close()
    }

    void setupVisitor(CodeObserverVisitor visitor) {
        this.visitor = Objects.requireNonNull(visitor)
    }

    void setupVisitor(CodeObserver... codeObservers) {
        if (session == null) {
            throw new IllegalStateException("Phenomena must be connected to Grakn before setting up the visitor")
        }

        visitor = new CodeObserverVisitor(session)
        codeObservers.each {
            visitor.addObserver(it)
        }
    }

    void setupOntology() {
        setupOntology(visitor.getObservers().toArray(new CodeObserver[0]))
    }

    void setupOntology(CodeObserver... codeObservers) {
        def stringBuilder = new StringBuilder()
        codeObservers.each {
            stringBuilder.append(it.getSchema().trim()).append(" ")
        }
        setupOntology(stringBuilder.toString())
    }

    void setupOntology(String schemaDefinition) {
        if (session == null) {
            throw new IllegalStateException("Phenomena must be connected to Grakn before setting up the ontology")
        }

        def tx = session.transaction(GraknTxType.WRITE)
        def graql = tx.graql()
        def query = graql.parse(schemaDefinition.replaceAll("[\\n\\r\\s](define)[\\n\\r\\s]", ""))
        query.execute()
        tx.commit()
    }

    Stream<ProcessedSourceFile> processScanPath() {
        if (visitor == null) {
            throw new IllegalStateException("Phenomena must be initialized before processing source code")
        }
        return Streams.stream(new TransformIterator(sourceFilesInScanPath.iterator(), new ProcessSourceFileTransformer()))
    }

    ProcessedSourceFile processSourceFile(File sourceFile, SourceLanguage language) {
        if (visitor == null) {
            throw new IllegalStateException("Phenomena must be initialized before processing source code")
        }

        def resp = parseSourceFile(sourceFile, language)
        println "Processing $language file: " + sourceFile
        visitor.visit(language, resp.uast, sourceFile)

        def processedFile = new ProcessedSourceFile()
        if (visitor.saveToGrakn) {
            processedFile.rootNodeId = Optional.of(visitor.getContextualNode(resp.uast).getData(CodeStructureObserver.SELF_ID))
        }
        processedFile.sourceFile = sourceFile
        processedFile.parseResponse = resp
        return processedFile
    }

    Stream<ParseResponse> parseScanPath() {
        if (parser == null) {
            throw new IllegalStateException("Phenomena must be connected to Babelfish before parsing source code")
        }
        return Streams.stream(new TransformIterator(sourceFilesInScanPath.iterator(), new ParseSourceFileTransformer()))
    }

    ParseResponse parseSourceFile(File sourceFile, SourceLanguage language) {
        if (parser == null) {
            throw new IllegalStateException("Phenomena must be connected to Babelfish before parsing source code")
        }

        println "Parsing $language file: " + sourceFile
        return parser.parse(sourceFile.name, sourceFile.text, language.key, Encoding.UTF8$.MODULE$)
    }

    List<File> getSourceFilesInScanPath() {
        def rtnList = new ArrayList<File>()
        if (scanPath != null && !scanPath.isEmpty()) {
            scanPath.each {
                def file = new File(it)
                if (file.isDirectory()) {
                    file.eachFileRecurse(FILES) {
                        if (SourceLanguage.isSourceLanguageKnown(it)) {
                            rtnList.add(it)
                        }
                    }
                } else if (SourceLanguage.isSourceLanguageKnown(file)) {
                    rtnList.add(file)
                }
            }
        }
        return rtnList
    }

    void close() {
        parser?.close()
        session?.close()
    }

    void addCodeObserver(CodeObserver codeObserver) {
        visitor.addObserver(codeObserver)
    }

    List<String> getScanPath() {
        return scanPath
    }

    void setScanPath(List<String> scanPath) {
        this.scanPath = scanPath
    }

    List<String> getActiveObservers() {
        return activeObservers
    }

    void setActiveObservers(List<String> activeObservers) {
        this.activeObservers = activeObservers
    }

    String getGraknHost() {
        return graknHost
    }

    void setGraknHost(String graknHost) {
        this.graknHost = graknHost
    }

    int getGraknPort() {
        return graknPort
    }

    void setGraknPort(int graknPort) {
        this.graknPort = graknPort
    }

    String getGraknKeyspace() {
        return graknKeyspace
    }

    void setGraknKeyspace(String graknKeyspace) {
        this.graknKeyspace = graknKeyspace
    }

    String getBabelfishHost() {
        return babelfishHost
    }

    void setBabelfishHost(String babelfishHost) {
        this.babelfishHost = babelfishHost
    }

    int getBabelfishPort() {
        return babelfishPort
    }

    void setBabelfishPort(int babelfishPort) {
        this.babelfishPort = babelfishPort
    }

    String getGraknURI() {
        return graknHost + ":" + graknPort
    }

    private class ProcessSourceFileTransformer implements Transformer<File, ProcessedSourceFile> {
        @Override
        ProcessedSourceFile transform(File file) {
            return processSourceFile(file, SourceLanguage.getSourceLanguage(file))
        }
    }

    private class ParseSourceFileTransformer implements Transformer<File, ParseResponse> {
        @Override
        ParseResponse transform(File file) {
            return parseSourceFile(file, SourceLanguage.getSourceLanguage(file))
        }
    }
}
