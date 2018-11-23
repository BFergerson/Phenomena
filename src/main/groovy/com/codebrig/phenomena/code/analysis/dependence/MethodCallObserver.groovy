package com.codebrig.phenomena.code.analysis.dependence

import com.codebrig.phenomena.code.CodeObserver
import com.google.common.base.Charsets
import com.google.common.io.Resources

/**
 * Creates edges between method call statements and the methods they call
 *
 * @version 0.2
 * @since 0.2
 * @author <a href="mailto:brandon.fergerson@codebrig.com">Brandon Fergerson</a>
 */
abstract class MethodCallObserver implements CodeObserver {
    @Override
    String getSchema() {
        return Resources.toString(Resources.getResource(
                "schema/dependence/method-call-schema.gql"), Charsets.UTF_8)
    }

    static String getFullSchema() {
        //todo: smarter
        def schemaDefinition = Resources.toString(Resources.getResource(
                "schema/dependence/method-call-schema.gql"), Charsets.UTF_8) + " "
        schemaDefinition += Resources.toString(Resources.getResource(
                "schema/dependence/language/java/method-call-schema.gql"), Charsets.UTF_8)
        return schemaDefinition.replaceAll("[\\n\\r\\s](define)[\\n\\r\\s]", "")
    }
}
