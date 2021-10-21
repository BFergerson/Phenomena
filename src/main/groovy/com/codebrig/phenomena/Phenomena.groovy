package com.codebrig.phenomena

import com.codebrig.arthur.SourceLanguage
import com.codebrig.phenomena.code.CodeObserver
import com.codebrig.phenomena.code.CodeObserverVisitor
import com.codebrig.phenomena.code.ProcessedSourceFile
import com.codebrig.phenomena.code.structure.CodeStructureObserver
import com.vaticle.typedb.client.api.connection.TypeDBClient
import com.vaticle.typedb.client.api.connection.TypeDBSession
import com.vaticle.typedb.client.api.connection.TypeDBTransaction
import com.vaticle.typedb.client.connection.core.CoreClient
import com.vaticle.typeql.lang.query.TypeQLDefine
import gopkg.in.bblfsh.sdk.v1.protocol.generated.Encoding
import gopkg.in.bblfsh.sdk.v1.protocol.generated.ParseResponse
import groovy.util.logging.Slf4j
import org.apache.commons.collections4.Transformer
import org.apache.commons.collections4.iterators.TransformIterator
import org.bblfsh.client.BblfshClient

import java.util.stream.Stream
import java.util.stream.StreamSupport

import static com.vaticle.typeql.lang.TypeQL.parseQuery
import static groovy.io.FileType.FILES

/**
 * Main entry-point used to parse, process, and store source code files
 *
 * @since 0.1
 * @author <a href="mailto:brandon.fergerson@codebrig.com">Brandon Fergerson</a>
 */
@Slf4j
class Phenomena implements Closeable {

    private static ResourceBundle buildBundle = ResourceBundle.getBundle("phenomena_build")
    public static final String PHENOMENA_VERSION = buildBundle.getString("version")
    private List<String> scanPath
    private List<String> activeObservers = ["structure"]
    private String graknHost = "localhost"
    private int graknPort = 1729
    private String graknKeyspace = "grakn"
    private String babelfishHost = "localhost"
    private int babelfishPort = 9432
    private CodeObserverVisitor visitor
    private BblfshClient babelfishClient
    private TypeDBClient graknClient
    private TypeDBSession schemaSession
    private TypeDBSession dataSession

    void init() throws ConnectionException {
        init(new CodeStructureObserver())
    }

    void init(List<CodeObserver> codeObservers) throws ConnectionException {
        Objects.requireNonNull(codeObservers)
        init(codeObservers.toArray(new CodeObserver[0]) as CodeObserver[])
    }

    void init(CodeObserver... codeObservers) throws ConnectionException {
        if (codeObservers.length == 0) {
            throw new IllegalArgumentException("Missing code observers")
        }

        log.info "Initializing Phenomena (ver.$PHENOMENA_VERSION)"
        connectToBabelfish()
        connectToGrakn()
        setupVisitor(codeObservers)
    }

    void connectToBabelfish() throws ConnectionException {
        log.info "Connecting to Babelfish"
        babelfishClient = new BblfshClient(babelfishHost, babelfishPort, Integer.MAX_VALUE)
        try {
            babelfishClient.supportedLanguages()
        } catch (Throwable ex) {
            throw new ConnectionException("Connection refused: $babelfishHost:$babelfishPort", ex)
        }
    }

    void connectToGrakn() throws ConnectionException {
        log.info "Connecting to Grakn"
        graknClient = new CoreClient("$graknURI")
        try {
            schemaSession = graknClient.session(graknKeyspace, TypeDBSession.Type.SCHEMA)
        } catch (Throwable ex) {
            throw new ConnectionException("Connection refused: $graknURI", ex)
        }
    }

    void setupVisitor(CodeObserverVisitor visitor) {
        this.visitor = Objects.requireNonNull(visitor)
    }

    void setupVisitor(List<CodeObserver> codeObservers) {
        setupVisitor(codeObservers as CodeObserver[])
    }

    void setupVisitor(CodeObserver... codeObservers) {
        if (graknClient == null) {
            throw new IllegalStateException("Phenomena must be connected to Grakn before setting up the visitor")
        }

        visitor = new CodeObserverVisitor(graknClient, graknKeyspace)
        codeObservers.each {
            visitor.addObserver(it)
        }
    }

    void setupOntology() {
        setupOntology(visitor.getObservers().toArray(new CodeObserver[0]) as CodeObserver[])
    }

    void setupOntology(CodeObserver... codeObservers) {
        def stringBuilder = new StringBuilder()
        codeObservers.each {
            stringBuilder.append(it.getSchema().trim()).append(" ")
            it.getRules().each {
                stringBuilder.append(it.trim()).append(" ")
            }
        }
        setupOntology(stringBuilder.toString())
        schemaSession.close()
    }

    void setupOntology(File schemaDefinition) {
        setupOntology(Objects.requireNonNull(schemaDefinition).text)
    }

    void setupOntology(String schemaDefinition) {
        if (schemaSession == null) {
            throw new IllegalStateException("Phenomena must be connected to Grakn before setting up the ontology")
        }

        def tx = schemaSession.transaction(TypeDBTransaction.Type.WRITE)
        def query = parseQuery(schemaDefinition.replaceAll("[\\n\\r\\s](define)[\\n\\r\\s]", ""))
        tx.query().define(query as TypeQLDefine)
        tx.commit()
    }

    Stream<ProcessedSourceFile> processScanPath() throws ParseException {
        if (visitor == null) {
            throw new IllegalStateException("Phenomena must be initialized before processing source code")
        }
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(
                new TransformIterator(sourceFilesInScanPath.iterator(), new ProcessSourceFileTransformer()),
                Spliterator.ORDERED), false)
    }

    ProcessedSourceFile processSourceFile(File sourceFile, SourceLanguage language) throws ParseException {
        if (visitor == null) {
            throw new IllegalStateException("Phenomena must be initialized before processing source code")
        }

        def resp = parseSourceFile(sourceFile, language)
        if (!resp.status().isOk()) {
            throw new ParseException("Failed to parse: $sourceFile\nReason(s): " + resp.errors().toString(),
                    resp, sourceFile)
        }

        log.info "Processing $language file: " + sourceFile
        def rootObservedNode = visitor.visit(language, resp.uast, sourceFile)

        def processedFile = new ProcessedSourceFile()
        if (visitor.saveToGrakn) {
            processedFile.rootNodeId = Optional.ofNullable(rootObservedNode?.getData(CodeStructureObserver.SELF_ID))
        }
        processedFile.sourceFile = sourceFile
        processedFile.parseResponse = resp
        return processedFile
    }

    Stream<ParseResponse> parseScanPath() {
        if (babelfishClient == null) {
            throw new IllegalStateException("Phenomena must be connected to Babelfish before parsing source code")
        }
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(
                new TransformIterator(sourceFilesInScanPath.iterator(), new ParseSourceFileTransformer()),
                Spliterator.ORDERED), false)
    }

    ParseResponse parseSourceFile(File sourceFile, SourceLanguage language) {
        if (babelfishClient == null) {
            throw new IllegalStateException("Phenomena must be connected to Babelfish before parsing source code")
        }

        log.info "Parsing $language file: " + sourceFile
        return babelfishClient.parse(sourceFile.name, sourceFile.text, language.babelfishName, Encoding.UTF8$.MODULE$)
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

    BblfshClient getBabelfishClient() {
        return babelfishClient
    }

    TypeDBClient getGraknClient() {
        return graknClient
    }

    TypeDBSession getSchemaSession() {
        return schemaSession
    }

    TypeDBSession getDataSession() {
        return dataSession
    }

    @Override
    void close() {
        visitor?.observers?.each {
            it.reset()
        }
        babelfishClient?.close()
        schemaSession?.close()
        visitor?.dataSession?.close() //todo: better
        graknClient?.close()
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
