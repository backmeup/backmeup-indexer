package org.backmeup.index.query;

import java.util.Date;

import org.elasticsearch.cluster.ClusterState;

public class ESClusterStateCache {

    private Date executionDate;
    private Date expirationDate;
    private Long userID;
    private ClusterState clusterState;

    public ESClusterStateCache(Long userID, int TTLSeconds, ClusterState clusterstate) {
        this.userID = userID;
        this.executionDate = new Date(System.currentTimeMillis());
        this.setExpirationDate(TTLSeconds);
        this.clusterState = clusterstate;
    }

    public boolean isCacheExpired() {
        Date now = new Date(System.currentTimeMillis());
        return now.after(this.expirationDate);
    }

    private void setExpirationDate(int TTLSeconds) {
        this.expirationDate = new Date(this.executionDate.getTime() + TTLSeconds * 1000);
    }

    public ClusterState getClusterState() {
        return this.clusterState;
    }

    public Long getUserID() {
        return this.userID;
    }

}
