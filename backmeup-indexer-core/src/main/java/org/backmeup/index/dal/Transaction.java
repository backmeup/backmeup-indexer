package org.backmeup.index.dal;

import java.util.concurrent.Callable;

/**
 * Starts and stops transactions.
 * 
 * @author <a href="http://www.code-cop.org/">Peter Kofler</a>
 */
public interface Transaction {

    <T> T inside(Callable<T> getter);

    void inside(Runnable call);

}