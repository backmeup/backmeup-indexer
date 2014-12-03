package org.backmeup.index.client;

/**
 * There was a problem in the index client, e.g. could not connect to index
 * server component.
 * 
 * @author <a href="http://www.code-cop.org/">Peter Kofler</a>
 */
public class IndexClientException extends RuntimeException {

    public IndexClientException(String message) {
        super(message);
    }

    public IndexClientException(String message, Throwable cause) {
        super(message, cause);
    }

}
