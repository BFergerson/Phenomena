package com.codebrig.phenomena.code.analysis

import com.codebrig.arthur.SourceLanguage
import com.codebrig.phenomena.Phenomena
import com.codebrig.phenomena.code.CodeObserver
import com.codebrig.phenomena.code.analysis.language.java.JavaParserIntegration
import com.codebrig.phenomena.code.analysis.language.java.dependence.JavaIdentifierAccessObserver
import com.codebrig.phenomena.code.analysis.language.java.dependence.JavaMethodCallObserver
import com.google.common.base.Charsets
import com.google.common.io.Resources

import static com.codebrig.arthur.SourceLanguage.Java
import static com.codebrig.arthur.SourceLanguage.Omnilingual

/**
 * todo: description
 *
 * @version 0.2.4
 * @since 0.2
 * @author <a href="mailto:brandon.fergerson@codebrig.com">Brandon Fergerson</a>
 */
enum DependenceAnalysis {

    Identifier_Access(Java),
    Method_Call(Java)

    private final List<SourceLanguage> supportedLanguages

    DependenceAnalysis(SourceLanguage... supportedLanguages) {
        this(Arrays.asList(supportedLanguages))
    }

    DependenceAnalysis(List<SourceLanguage> supportedLanguages) {
        this.supportedLanguages = supportedLanguages
    }

    String getSchemaDefinition() {
        def analysisType = name().toLowerCase().replace("_", "-")
        def schemaDefinition = Resources.toString(Resources.getResource(
                "schema/dependence/$analysisType-schema.gql"), Charsets.UTF_8) + " "
        supportedLanguages.each {
            if (it != Omnilingual) {
                schemaDefinition += Resources.toString(Resources.getResource(
                        "schema/dependence/language/" + it.key + "/$analysisType-schema.gql"), Charsets.UTF_8)
            }
        }
        return schemaDefinition.replaceAll("[\\n\\r\\s](define)[\\n\\r\\s]", "")
    }

    List<SourceLanguage> getSupportedLanguages() {
        return supportedLanguages
    }

    List<CodeObserver> getCodeObservers(Phenomena phenomena) {
        return getCodeObserversByLanguage(phenomena, SourceLanguage.supportedLanguages)
    }

    List<CodeObserver> getCodeObserversByLanguage(Phenomena phenomena, SourceLanguage... sourceLanguages) {
        getCodeObserversByLanguage(phenomena, Arrays.asList(sourceLanguages))
    }

    List<CodeObserver> getCodeObserversByLanguage(Phenomena phenomena, List<SourceLanguage> sourceLanguages) {
        getCodeObservers(phenomena, sourceLanguages, Collections.singletonList(this))
    }

    static List<CodeObserver> getCodeObserversByAnalysis(Phenomena phenomena,
                                                         List<DependenceAnalysis> dependenceAnalyses) {
        return getCodeObservers(phenomena, SourceLanguage.supportedLanguages, dependenceAnalyses)
    }

    static List<CodeObserver> getCodeObservers(Phenomena phenomena, List<SourceLanguage> sourceLanguages,
                                               DependenceAnalysis... dependenceAnalyses) {
        return getCodeObservers(phenomena, sourceLanguages, Arrays.asList(dependenceAnalyses))
    }

    static List<CodeObserver> getCodeObservers(Phenomena phenomena, List<SourceLanguage> sourceLanguages,
                                               List<DependenceAnalysis> dependenceAnalyses) {
        def javaParserIntegration = null
        def codeObservers = new ArrayList<>()

        dependenceAnalyses.each {
            switch (it) {
                case Identifier_Access:
                    if (Java in sourceLanguages) {
                        if (javaParserIntegration == null) {
                            javaParserIntegration = new JavaParserIntegration(phenomena)
                        }
                        codeObservers.add(new JavaIdentifierAccessObserver(javaParserIntegration))
                    }
                    break
                case Method_Call:
                    if (Java in sourceLanguages) {
                        if (javaParserIntegration == null) {
                            javaParserIntegration = new JavaParserIntegration(phenomena)
                        }
                        codeObservers.add(new JavaMethodCallObserver(javaParserIntegration))
                    }
                    break
                default:
                    throw new UnsupportedOperationException()
            }
        }
        return codeObservers
    }

    static List<CodeObserver> getAllCodeObservers(Phenomena phenomena) {
        return getCodeObservers(phenomena, SourceLanguage.supportedLanguages, Arrays.asList(values()))
    }

    static List<CodeObserver> getAllCodeObservers(Phenomena phenomena, SourceLanguage... sourceLanguages) {
        return getCodeObservers(phenomena, Arrays.asList(sourceLanguages), Arrays.asList(values()))
    }

    static List<CodeObserver> getAllCodeObservers(Phenomena phenomena, List<SourceLanguage> sourceLanguages) {
        return getCodeObservers(phenomena, sourceLanguages, Arrays.asList(values()))
    }
}
