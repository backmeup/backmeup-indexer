package org.backmeup.index;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Startup and destroy of the application. This depends on a lot of state and
 * can not work in post construct and pre destroy.
 * 
 * @author <a href="http://www.code-cop.org/">Peter Kofler</a>
 */
public class IndexManagerLifeCycle {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    private IndexManager indexManager;
    @Inject
    private IndexCoreGarbageCollector cleanupTask;

    public void initialized() {
        this.log.debug(">>>>> Startup IndexManager >>>>>");

        indexManager.startupIndexManager();

        this.cleanupTask.init();

        this.log.debug(">>>>> Startup IndexManager DONE >>>>>");
    }

    public void destroyed() {
        this.log.debug(">>>>> Shutdown IndexManager >>>>>");

        this.cleanupTask.end();

        indexManager.shutdownIndexManager();

        this.log.debug(">>>>> Shutdown IndexManager DONE >>>>>");
    }

}
