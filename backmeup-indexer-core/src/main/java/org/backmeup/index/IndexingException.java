package org.backmeup.index;

public class IndexingException extends RuntimeException {

    private static final long serialVersionUID = 3110030867711670828L;

    public IndexingException(String arg0, Throwable arg1) {
        super(arg0, arg1);
    }

    public IndexingException(String arg0) {
        super(arg0);
    }

    public IndexingException(Throwable arg0) {
        super(arg0);
    }

}
