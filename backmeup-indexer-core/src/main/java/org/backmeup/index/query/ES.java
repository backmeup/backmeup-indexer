package org.backmeup.index.query;

import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;

import org.backmeup.index.core.elasticsearch.SearchInstanceException;
import org.backmeup.index.core.model.RunningIndexUserConfig;
import org.backmeup.index.model.User;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.state.ClusterStateRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ES {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    public Client getESTransportClient(RunningIndexUserConfig conf) {
        //check if we've got a DB record
        if (conf == null) {
            throw new SearchInstanceException("Failed to create ES TransportClient " 
                    + " due to missing RunningIndexUserConfig");
        }

        Settings settings = ImmutableSettings.settingsBuilder().put("cluster.name", conf.getClusterName()).build();

        // now try to connect with the TransportClient - requires the
        // transport.tcp.port for connection
        @SuppressWarnings("resource") // this is a factory method
        Client client = new TransportClient(settings).addTransportAddress(new InetSocketTransportAddress(conf
                .getHostAddress().getHost(), conf.getTcpPort()));
        return client;
    }
    
    public void getESClusterState(User userID, Client client, int maxAttempts, int sleepSeconds) {
        int count = 1;
        while (true) {
            try {

                //try to receive a clusterstate reply
                this.getESClusterState(userID, client);
                this.log.debug("startupInstance for userID: " + userID + " step7 - ok");
                return;

            } catch (SearchInstanceException e1) {
                String s = "startupInstance for userID: " + userID
                        + " step7 - waiting for cluster reply. number of attempts: " + count;
                this.log.debug(s, e1);

                if (count == maxAttempts) {
                    s = "startupInstance for userID: " + userID + " step7 - failed";
                    this.log.debug(s, e1);
                    throw e1;
                }
                try {
                    //give cluster a change to startup and wait - max x times
                    Thread.sleep(sleepSeconds * 1000);
                } catch (InterruptedException e) {
                }
            }
            count++;
        }
    }

    public ClusterState getESClusterState(User userId, Client client) {
        //request clusterstate and cluster health
        ClusterState clusterState = client.admin().cluster().state(new ClusterStateRequest())
                .actionGet(10, TimeUnit.SECONDS).getState();
        ClusterHealthResponse clusterHealthResponse = client.admin().cluster().health(new ClusterHealthRequest())
                .actionGet(10, TimeUnit.SECONDS);

        this.log.debug("get ES Cluster health state for userID: " + userId + " " + clusterHealthResponse.toString());
        return clusterState;
    }
    
}
