package org.backmeup.index;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class IndexKeepAliveTimer {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Map<Long, Date> lastAccessLog = new HashMap<>();
    private int minutes = 20;

    /**
     * Extends the time to life for a index instance with +20 minutes from now
     */
    public synchronized void extendTTL20(Long userID) {
        Date d = new Date(System.currentTimeMillis() + this.minutes * 60 * 1000);
        this.log.debug("IndexKeepAliveTimer extended ES Instance TTL for userID: " + userID + " until: "
                + getFormatedDate(d));

        this.lastAccessLog.put(userID, d);
    }

    /**
     * Returns a list of instances that can be shutdown as no request was made within a specific period of time
     * 
     * @return userID
     */
    public synchronized List<Long> getUsersToShutdown() {
        List<Long> ret = new ArrayList<>();
        this.log.debug("IndexKeepALiveTimer checking users to shutdown...");
        for (Map.Entry<Long, Date> entry : this.lastAccessLog.entrySet()) {
            Date timestamp = entry.getValue();
            this.log.debug("checking entries for userID: " + entry.getKey() + " and timestamp "
                    + getFormatedDate(timestamp));
            if (isOverdue(timestamp)) {
                //perform cleanup and take down from list
                ret.add(entry.getKey());
                this.log.debug("IndexKeepALiveTimer - adding userID: " + entry.getKey() + " in the list to shutdown");
            }
        }
        return ret;
    }

    /**
     * Mark a running index instance as shutdown. No cleanup required for these instances anymore
     */
    public synchronized void flagAsShutdown(Long userID) {
        if (this.lastAccessLog.containsKey(userID)) {
            this.lastAccessLog.remove(userID);
            this.log.debug("IndexKeepAliveTimer flag ES instances for userID: " + userID
                    + "as shutdown, removed reocrds in timer");
        }
    }

    /**
     * Returns the number of currently initialized instances, managed by this class
     * 
     * @return
     */
    public synchronized int countOpenInstances() {
        return this.lastAccessLog.size();
    }

    private boolean isOverdue(Date testDate) {
        return testDate.before(new Date(System.currentTimeMillis() - this.minutes * 60 * 1000));
    }

    private String getFormatedDate(Date d) {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss.SSS");
        return sdf.format(d);
    }
}
