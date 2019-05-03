package com.codebrig.phenomena.code.analysis

import com.codebrig.arthur.SourceLanguage
import com.codebrig.phenomena.Phenomena
import com.codebrig.phenomena.code.CodeObserver
import com.codebrig.phenomena.code.analysis.metric.CyclomaticComplexityObserver
import com.google.common.base.Charsets
import com.google.common.io.Resources

import static com.codebrig.arthur.SourceLanguage.Omnilingual

/**
 * todo: description
 *
 * @version 0.2.3
 * @since 0.2
 * @author <a href="mailto:brandon.fergerson@codebrig.com">Brandon Fergerson</a>
 */
enum MetricAnalysis {

    Cyclomatic_Complexity(Omnilingual)

    private final List<SourceLanguage> supportedLanguages

    MetricAnalysis(SourceLanguage... supportedLanguages) {
        this(Arrays.asList(supportedLanguages))
    }

    MetricAnalysis(List<SourceLanguage> supportedLanguages) {
        this.supportedLanguages = supportedLanguages
    }

    String getSchemaDefinition() {
        def analysisType = name().toLowerCase().replace("_", "-")
        def schemaDefinition = Resources.toString(Resources.getResource(
                "schema/metric/$analysisType-schema.gql"), Charsets.UTF_8) + " "
        supportedLanguages.each {
            if (it != SourceLanguage.OmniSRC) {
                schemaDefinition += Resources.toString(Resources.getResource(
                        "schema/metric/language/" + it.key + "/$analysisType-schema.gql"), Charsets.UTF_8)
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

    static List<CodeObserver> getCodeObserversByAnalysis(Phenomena phenomena, List<MetricAnalysis> metricAnalyses) {
        return getCodeObservers(phenomena, SourceLanguage.supportedLanguages, metricAnalyses)
    }

    static List<CodeObserver> getCodeObservers(Phenomena phenomena, List<SourceLanguage> sourceLanguages,
                                               MetricAnalysis... metricAnalyses) {
        return getCodeObservers(phenomena, sourceLanguages, Arrays.asList(metricAnalyses))
    }

    static List<CodeObserver> getCodeObservers(Phenomena phenomena, List<SourceLanguage> sourceLanguages,
                                               List<MetricAnalysis> metricAnalyses) {
        def codeObservers = new ArrayList<>()
        metricAnalyses.each {
            switch (it) {
                case Cyclomatic_Complexity:
                    codeObservers.add(new CyclomaticComplexityObserver())
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
