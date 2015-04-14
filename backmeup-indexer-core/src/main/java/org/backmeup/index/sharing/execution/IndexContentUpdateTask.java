package org.backmeup.index.sharing.execution;

import java.util.List;

import javax.inject.Inject;

import org.backmeup.index.ActiveUsers;
import org.backmeup.index.model.User;
import org.backmeup.index.utils.cdi.RunRequestScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndexContentUpdateTask implements Runnable {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    private ActiveUsers activeUsers;
    @Inject
    private IndexContentManager contentManager;

    @Override
    @RunRequestScoped
    public void run() {
        executeCheckForContentUpdates();
    }

    public void executeCheckForContentUpdates() {
        List<User> activeUsers = this.activeUsers.getActiveUsers();
        this.log.debug("started running...checking for active users and IndexDocument import/deletion tasks found: "
                + activeUsers.size() + " active users");

        for (User user : activeUsers) {
            this.log.debug("checking for new index documents to import/delete for user: " + user.id());
            //call the content manager object to check for import / delete operations to execute
            this.contentManager.executeContentUpdates(user);
        }
    }

}