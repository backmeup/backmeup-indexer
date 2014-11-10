package org.backmeup.index;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//@ApplicationScoped
public class IndexKeepAliveTimer {

    private Map<Long, Date> lastAccessLog = new HashMap<Long, Date>();

    private static IndexKeepAliveTimer tm = new IndexKeepAliveTimer();
    private int minutes = 20;

    private IndexKeepAliveTimer() {
    }

    public static IndexKeepAliveTimer getInstance() {
        return tm;
    }

    /**
     * Extends the time to life for a index instance with +20 minutes from now
     * 
     * @param userID
     */
    public void extendTTL20(Long userID) {
        Date d = new Date(System.currentTimeMillis() + this.minutes * 60 * 1000);
    }

    /**
     * Returns a list of instances that can be shutdown as no request was made within a specific period of time
     * 
     * @return userID
     */
    public List<Long> getUsersToShutdown() {
        List<Long> ret = new ArrayList<Long>();
        for (Map.Entry<Long, Date> entry : this.lastAccessLog.entrySet()) {
            Date timestamp = entry.getValue();
            if (isOverdue(timestamp)) {
                //perform cleanup and take down from list
                ret.add(entry.getKey());
            }
        }
        return ret;
    }

    public void flagAsShutdown(Long userID) {
        if (this.lastAccessLog.containsKey(userID)) {
            this.lastAccessLog.remove(userID);
        }
    }

    private boolean isOverdue(Date testDate) {
        if (testDate.before(new Date(System.currentTimeMillis() - this.minutes * 60 * 1000))) {
            return true;
        } else {
            return false;
        }
    }
}
