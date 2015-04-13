package org.backmeup.index.sharing.execution;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.inject.Inject;

import org.backmeup.index.ActiveUsers;
import org.backmeup.index.model.User;
import org.backmeup.index.utils.cdi.RunRequestScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndexContentUpdateTask {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ScheduledExecutorService exec = Executors.newScheduledThreadPool(1);
    private int SECONDS_BETWEEN_RECHECKING = 30;

    @Inject
    private ActiveUsers activeUsers;
    @Inject
    private IndexContentManager contentManager;

    @RunRequestScoped
    public void startupCheckingForContentUpdates() {
        startChecking();
        this.log.debug("startup() IndexDocumentCheckForImports (ApplicationScoped) completed");
    }

    @RunRequestScoped
    public void shutdownCheckingForContentUpdates() {
        stopChecking();
        this.log.debug("shutdown() IndexDocumentCheckForImports (ApplicationScoped) completed");
    }

    private void startChecking() {

        this.log.debug("IndexDocumentCheckForImports starting thread responsible for ES import/deletion checking");
        this.exec.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                executeCheckForContentUpdates();
            }
        }, this.SECONDS_BETWEEN_RECHECKING, this.SECONDS_BETWEEN_RECHECKING, java.util.concurrent.TimeUnit.SECONDS);
    }

    public void executeCheckForContentUpdates() {
        this.log.debug("started running...checking for active users and IndexDocument import/deletion tasks");
        List<User> activeUsers = this.activeUsers.getActiveUsers();

        for (User user : activeUsers) {
            this.log.info("checking for new index documents to import/delete for user: " + user.id());
            //call the content manager object to check for import / delete operations to execute
            this.contentManager.executeContentUpdates(user);
        }
    }

    public void stopChecking() {
        this.log.debug("IndexDocumentCheckForImports stopping thread responsible for ES import/deletion checking");
        this.exec.shutdownNow();
    }

    public void setCheckingFrequency(int secs) {
        this.SECONDS_BETWEEN_RECHECKING = secs;
    }
}