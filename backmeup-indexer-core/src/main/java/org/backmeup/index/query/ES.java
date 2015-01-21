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
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.RemoteTransportException;
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
        @SuppressWarnings("resource")
        // this is a factory method
        Client client = new TransportClient(settings).addTransportAddress(new InetSocketTransportAddress(conf
                .getHostAddress().getHost(), conf.getTcpPort()));
        return client;
    }

    /**
     * Checks on the clusterstate of an ES instance for a given userID + client. This method might throw a
     * SearchInstanceException when max attempts reached and cluster not responding
     * 
     * @param userID
     * @param client
     * @param maxAttempts
     * @param sleepSeconds
     */
    public ClusterState getESClusterState(User userID, Client client, int maxAttempts, int sleepSeconds) {
        int count = 1;
        while (true) {
            try {

                //try to receive a clusterstate reply
                this.log.debug("checking cluster state for userID " + userID);
                return this.getESClusterState(client);

            } catch (SearchInstanceException e1) {
                String s = "cluster state reply for userID " + userID + " not responding. number of attempts: " + count
                        + " out of " + maxAttempts;
                this.log.debug(s);

                if (count == maxAttempts) {
                    s = "cluster state reply for userID " + userID + " failed. cluster not responding.";
                    this.log.debug(s, e1);
                    throw new SearchInstanceException(s);
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

    /**
     * Retrieve the clusterstate and cluster health response of a given ES instance. This method might throw a
     * SearchInstanceException when max attempts reached
     **/
    private ClusterState getESClusterState(Client client) {
        try {
            //request clusterstate and cluster health
            ClusterState clusterState = client.admin().cluster().state(new ClusterStateRequest())
                    .actionGet(10, TimeUnit.SECONDS).getState();
            ClusterHealthResponse clusterHealthResponse = client.admin().cluster().health(new ClusterHealthRequest())
                    .actionGet(10, TimeUnit.SECONDS);

            this.log.debug("ES cluster health response: " + clusterHealthResponse.toString());
            return clusterState;
        } catch (NoNodeAvailableException | RemoteTransportException e1) {
            throw new SearchInstanceException("ES cluster not responding", e1);
        }
    }

}
