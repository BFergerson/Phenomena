package com.codebrig.phenomena

import ai.grakn.GraknTxType
import ai.grakn.Keyspace
import ai.grakn.client.Grakn
import ai.grakn.util.SimpleURI
import com.codebrig.omnisrc.SourceLanguage
import com.codebrig.phenomena.code.CodeObserver
import com.codebrig.phenomena.code.CodeObserverVisitor
import com.codebrig.phenomena.code.ContextualNode
import com.codebrig.phenomena.code.ParsedSourceFile
import com.codebrig.phenomena.code.structure.CodeStructureObserver
import gopkg.in.bblfsh.sdk.v1.protocol.generated.Encoding
import gopkg.in.bblfsh.sdk.v1.protocol.generated.ParseResponse
import gopkg.in.bblfsh.sdk.v1.uast.generated.Node
import org.bblfsh.client.BblfshClient
import scala.collection.JavaConverters

import static groovy.io.FileType.FILES

/**
 * todo: description
 *
 * @version 0.1
 * @since 0.1
 * @author <a href="mailto:brandon.fergerson@codebrig.com">Brandon Fergerson</a>
 */
class Phenomena {

    private static ResourceBundle buildBundle = ResourceBundle.getBundle("phenomena_build")
    public static final String PHENOMENA_VERSION = buildBundle.getString("version")
    private List<String> scanPath
    private List<String> activeObservers = ["structure"] as List
    private String graknHost = "localhost"
    private int graknPort = 48555
    private String graknKeyspace = "grakn"
    private String babelfishHost = "localhost"
    private int babelfishPort = 9432
    private CodeObserverVisitor visitor
    private BblfshClient parser
    private Grakn.Session session

    void init() {
        println "Initializing Phenomena (ver.$PHENOMENA_VERSION)"
        visitor = new CodeObserverVisitor(Keyspace.of(graknKeyspace))
        visitor.addObserver(new CodeStructureObserver())
        connectToBabelfish()
        connectToGrakn()
    }

    void connectToBabelfish() {
        println "Connecting to Babelfish"
        parser = new BblfshClient(babelfishHost, babelfishPort, Integer.MAX_VALUE)
    }

    void connectToGrakn() {
        println "Connecting to Grakn"
        session = new Grakn(new SimpleURI(graknURI)).session(Keyspace.of(graknKeyspace))
    }

    void setupOntology() {
        setupOntology(visitor.getObservers().toArray(new CodeObserver[0]))
    }

    void setupOntology(CodeObserver... codeObservers) {
        if (session == null) {
            throw new IllegalStateException("Phenomena must be connected to Grakn before setting up the ontology")
        }

        println "Setting up ontology"
        def stringBuilder = new StringBuilder()
        codeObservers.each {
            stringBuilder.append(it.getSchema().trim()).append(" ")
        }

        def tx = session.transaction(GraknTxType.WRITE)
        def graql = tx.graql()
        def query = graql.parse(stringBuilder.toString().replaceAll("[\\n\\r\\s](define)[\\n\\r\\s]", ""))
        query.execute()
        tx.commit()
    }

    List<ParsedSourceFile> processScanPath() {
        if (visitor == null) {
            throw new IllegalStateException("Phenomena must be initialized before processing source code")
        }

        println "Processing scan path"
        def rtnList = new ArrayList<ParsedSourceFile>()
        sourceFilesInScanPath.each {
            rtnList.add(processSourceFile(it, SourceLanguage.getSourceLangauge(it)))

        }
        return rtnList
    }

    ParseResponse parseSourceFile(File sourceFile, SourceLanguage language) {
        if (parser == null) {
            throw new IllegalStateException("Phenomena must be connected to Babelfish before processing source code")
        }

        println "Parsing $language file: " + sourceFile
        return parser.parse(sourceFile.name, sourceFile.text, language.key(), Encoding.UTF8$.MODULE$)
    }

    ParsedSourceFile processSourceFile(File sourceFile, SourceLanguage language) {
        if (visitor == null) {
            throw new IllegalStateException("Phenomena must be initialized before processing source code")
        }

        def resp = parseSourceFile(sourceFile, language)
        println "Saving $language file: " + sourceFile
        ContextualNode rootNode

        def innerTx = session.transaction(GraknTxType.WRITE)
        asJavaIterator(BblfshClient.iterator(resp.uast, BblfshClient.PostOrder())).each {
            if (it != null) {
                visitor.visit(rootNode = ContextualNode.getContextualNode(language, it), innerTx)
            }
        }
        innerTx.commit()

        def parsedFile = new ParsedSourceFile()
        parsedFile.rootNodeId = Optional.of(rootNode.getData(CodeStructureObserver.SELF_ID))
        parsedFile.sourceFile = sourceFile
        parsedFile.parseResponse = resp
        return parsedFile
    }

    String processUAST(List<Node> uast, SourceLanguage language) {
        if (visitor == null) {
            throw new IllegalStateException("Phenomena must be initialized before processing source code")
        }

        ContextualNode rootNode
        def innerTx = session.transaction(GraknTxType.WRITE)
        uast.each {
            if (it != null) {
                visitor.visit(rootNode = ContextualNode.getContextualNode(language, it), innerTx)
            }
        }
        innerTx.commit()
        return rootNode.getData(CodeStructureObserver.SELF_ID)
    }

    List<File> getSourceFilesInScanPath() {
        def rtnList = new ArrayList<File>()
        if (scanPath != null && !scanPath.isEmpty()) {
            scanPath.each {
                new File(it).eachFileRecurse(FILES) {
                    if (SourceLanguage.isSourceLanguageKnown(it)) {
                        rtnList.add(it)
                    }
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

    private static <T> Iterator<T> asJavaIterator(scala.collection.Iterator<T> scalaIterator) {
        return JavaConverters.asJavaIteratorConverter(scalaIterator).asJava()
    }

}
