package com.codebrig.phenomena.code

import gopkg.in.bblfsh.sdk.v1.protocol.generated.ParseResponse
import groovy.transform.Canonical

/**
 * Represents a source code file which as been processed
 *
 * @version 0.2.3
 * @since 0.1
 * @author <a href="mailto:brandon.fergerson@codebrig.com">Brandon Fergerson</a>
 */
@Canonical
class ProcessedSourceFile {
    String rootNodeId
    File sourceFile
    ParseResponse parseResponse
}
