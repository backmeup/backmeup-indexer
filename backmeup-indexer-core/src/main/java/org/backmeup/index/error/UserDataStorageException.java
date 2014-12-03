package org.backmeup.index.error;

public class UserDataStorageException extends IndexManagerCoreException {

    public UserDataStorageException(String message, Throwable cause) {
        super(message, cause);
    }

    public UserDataStorageException(String message) {
        super(message);
    }

}
