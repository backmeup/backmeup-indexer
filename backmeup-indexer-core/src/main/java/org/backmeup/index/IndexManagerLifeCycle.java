package org.backmeup.index;

import javax.inject.Inject;

import org.backmeup.index.sharing.execution.IndexContentManager;
import org.backmeup.index.sharing.execution.IndexContentUpdateTask;
import org.backmeup.index.sharing.execution.IndexDocumentDropOffQueue;
import org.backmeup.index.sharing.execution.SharingPolicyExecutionTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Startup and destroy of the application. This depends on a lot of state and can not work in post construct and pre
 * destroy.
 * 
 * @author <a href="http://www.code-cop.org/">Peter Kofler</a>
 */
public class IndexManagerLifeCycle {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    private IndexManager indexManager;
    @Inject
    private IndexCoreGarbageCollector cleanupTask;
    @Inject
    private IndexDocumentDropOffQueue queue;
    @Inject
    private SharingPolicyExecutionTask distributor;
    @Inject
    private IndexContentManager contentManager;
    @Inject
    private IndexContentUpdateTask importTask;

    public void initialized() {
        this.log.debug(">>>>> Startup IndexManager >>>>>");

        this.indexManager.startupIndexManager();

        this.cleanupTask.init();

        this.queue.startupDroOffQueue();

        this.distributor.startupSharingPolicyExecution();

        this.importTask.startupCheckingForContentUpdates();

        this.contentManager.startupIndexContentManager();

        this.log.debug(">>>>> Startup IndexManager DONE >>>>>");
    }

    public void destroyed() {
        this.log.debug(">>>>> Shutdown IndexManager >>>>>");

        this.contentManager.shutdownIndexContentManager();

        this.importTask.shutdownCheckingForContentUpdates();

        this.distributor.shutdownSharingPolicyExecution();

        this.queue.shutdownDroOffQueue();

        this.cleanupTask.end();

        this.indexManager.shutdownIndexManager();

        this.log.debug(">>>>> Shutdown IndexManager DONE >>>>>");
    }

}
