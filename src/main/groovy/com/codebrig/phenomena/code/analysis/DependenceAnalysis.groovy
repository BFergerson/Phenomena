package com.codebrig.phenomena.code.analysis

import com.codebrig.omnisrc.SourceLanguage
import com.codebrig.phenomena.Phenomena
import com.codebrig.phenomena.code.CodeObserver
import com.codebrig.phenomena.code.analysis.language.java.JavaParserIntegration
import com.codebrig.phenomena.code.analysis.language.java.dependence.JavaIdentifierAccessObserver
import com.codebrig.phenomena.code.analysis.language.java.dependence.JavaMethodCallObserver

import static com.codebrig.omnisrc.SourceLanguage.Java

/**
 * todo: description
 *
 * @version 0.2
 * @since 0.2
 * @author <a href="mailto:brandon.fergerson@codebrig.com">Brandon Fergerson</a>
 */
enum DependenceAnalysis {

    Identifier_Access(Java),
    Method_Call(Java);

    private final List<SourceLanguage> supportedLanguages

    DependenceAnalysis(SourceLanguage... supportedLanguages) {
        this(Arrays.asList(supportedLanguages))
    }

    DependenceAnalysis(List<SourceLanguage> supportedLanguages) {
        this.supportedLanguages = supportedLanguages
    }

    List<SourceLanguage> getSupportedLanguages() {
        return supportedLanguages
    }

    List<CodeObserver> getCodeObservers(Phenomena phenomena) {
        return getCodeObservers(phenomena, SourceLanguage.supportedLanguages)
    }

    List<CodeObserver> getCodeObservers(Phenomena phenomena, SourceLanguage... sourceLanguages) {
        return getCodeObservers(phenomena, Arrays.asList(sourceLanguages))
    }

    List<CodeObserver> getCodeObservers(Phenomena phenomena, List<SourceLanguage> sourceLanguages) {
        getCodeObservers(phenomena, sourceLanguages, this)
    }

    static List<CodeObserver> getCodeObservers(Phenomena phenomena, DependenceAnalysis... dependenceAnalyses) {
        return getCodeObservers(phenomena, Arrays.asList(dependenceAnalyses))
    }

    static List<CodeObserver> getCodeObservers(Phenomena phenomena, List<DependenceAnalysis> dependenceAnalyses) {
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
}
