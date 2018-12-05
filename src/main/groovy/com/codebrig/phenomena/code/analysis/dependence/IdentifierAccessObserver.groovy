package com.codebrig.phenomena.code.analysis.dependence

import com.codebrig.phenomena.code.CodeObserver
import com.google.common.base.Charsets
import com.google.common.io.Resources

/**
 * Creates edges between variable usages and their declarations
 *
 * @version 0.2
 * @since 0.2
 * @author <a href="mailto:brandon.fergerson@codebrig.com">Brandon Fergerson</a>
 */
abstract class IdentifierAccessObserver extends CodeObserver {
    @Override
    String getSchema() {
        return Resources.toString(Resources.getResource(
                "schema/dependence/identifier-access-schema.gql"), Charsets.UTF_8)
    }
}
