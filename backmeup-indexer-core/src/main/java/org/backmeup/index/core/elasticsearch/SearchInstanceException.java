package org.backmeup.index.core.elasticsearch;

import org.backmeup.index.error.IndexManagerCoreException;

public class SearchInstanceException extends IndexManagerCoreException {

    public SearchInstanceException(String message, Throwable cause) {
        super(message, cause);
    }

    public SearchInstanceException(String message) {
        super(message);
    }

}
