package com.codebrig.phenomena

/**
 * Thrown when the connection to a service (Grakn/Bbblsh) is or becomes unavailable.
 *
 * @since 0.3.0
 * @author <a href="mailto:brandon.fergerson@codebrig.com">Brandon Fergerson</a>
 */
class ConnectionException extends RuntimeException {

    ConnectionException(String message, Throwable cause) {
        super(message, cause)
    }
}
