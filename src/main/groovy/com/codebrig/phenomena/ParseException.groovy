package com.codebrig.phenomena

import gopkg.in.bblfsh.sdk.v1.protocol.generated.ParseResponse

/**
 * Thrown when a source code file fails to parse during processing
 *
 * @version 0.2
 * @since 0.2
 * @author <a href="mailto:brandon.fergerson@codebrig.com">Brandon Fergerson</a>
 */
class ParseException extends RuntimeException {

    private ParseResponse parseResponse
    private File sourceFile

    ParseException(String message, ParseResponse parseResponse, File sourceFile) {
        super(Objects.requireNonNull(message))
        this.parseResponse = Objects.requireNonNull(parseResponse)
        this.sourceFile = sourceFile
    }

    ParseResponse getParseResponse() {
        return parseResponse
    }

    File getSourceFile() {
        return sourceFile
    }
}
