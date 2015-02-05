package org.backmeup.index.sharing.execution;

import java.util.List;

import javax.inject.Inject;

import org.backmeup.index.ActiveUsers;
import org.backmeup.index.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndexDocumentCheckNewImportsTask implements Runnable {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    private ActiveUsers activeUsers;

    @Override
    public void run() {
        this.log.debug("started running...checking for active users and new Index Document import tasks");
        List<User> userIDs = this.activeUsers.getActiveUsers();

        for (User userId : userIDs) {
            this.log.info("checking for new index documents to import/delete for user: " + userId);

            //TODO need to add hook for importing here
        }
    }
}