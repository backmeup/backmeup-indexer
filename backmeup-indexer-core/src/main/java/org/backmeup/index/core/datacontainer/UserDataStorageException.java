package org.backmeup.index.core.datacontainer;

import org.backmeup.index.error.IndexManagerCoreException;

public class UserDataStorageException extends IndexManagerCoreException {

    public UserDataStorageException(String message, Throwable cause) {
        super(message, cause);
    }

    public UserDataStorageException(String message) {
        super(message);
    }

}
