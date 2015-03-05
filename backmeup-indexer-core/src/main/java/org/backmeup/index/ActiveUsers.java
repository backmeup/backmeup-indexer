package org.backmeup.index;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.backmeup.index.core.model.RunningIndexUserConfig;
import org.backmeup.index.dal.RunningIndexUserConfigDao;
import org.backmeup.index.model.User;

/**
 * Checks the DB if an ES instance is up and running for a given user
 *
 */
@ApplicationScoped
public class ActiveUsers {

    @Inject
    private RunningIndexUserConfigDao dao;

    public boolean isUserActive(User userID) {
        RunningIndexUserConfig config = this.dao.findConfigByUser(userID);
        if (config == null) {
            return false;
        } else {
            return true;
        }
    }

    public List<User> getActiveUsers() {
        List<User> ret = new ArrayList<User>();
        List<RunningIndexUserConfig> configs = this.dao.getAllESInstanceConfigs();
        for (RunningIndexUserConfig config : configs) {
            ret.add(config.getUser());
        }
        return ret;
    }

    public String getMountedDrive(User user) {
        RunningIndexUserConfig config = this.dao.findConfigByUser(user);
        if (config != null) {
            return config.getMountedTCDriveLetter();
        } else {
            return null;
        }
    }

    protected void setDaoForTesting(RunningIndexUserConfigDao dao) {
        this.dao = dao;
    }

}
