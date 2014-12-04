package org.backmeup.index;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Date;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.backmeup.index.core.elasticsearch.ESConfigurationHandler;
import org.backmeup.index.core.model.RunningIndexUserConfig;
import org.backmeup.index.error.IndexManagerCoreException;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class IndexManagerTest extends IndexManagerSetup {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void testESandTCLaunchTest() throws IndexManagerCoreException, IOException {
        this.indexManager.startupInstance(999992);

        RunningIndexUserConfig conf = this.indexManager.getRunningIndexUserConfig(999992);
        int httpPort = conf.getHttpPort();
        String drive = conf.getMountedTCDriveLetter();
        Assert.assertNotNull("mounting TC data drive for user should not fail", drive);
        Assert.assertTrue("ES http portnumber should have been assigned", httpPort > -1);
        System.out.println("user 999992 on port: " + httpPort + " and TC volume: " + drive);

        // check instance up and running
        Assert.assertTrue("ES Instance is not running ",
                ESConfigurationHandler.isElasticSearchInstanceRunning(conf.getHostAddress(), httpPort));
    }

    @Test
    public void testUserStartupArtefakts() throws IndexManagerCoreException {
        this.indexManager.startupInstance(999991);
        File fTC = new File(IndexManager.getUserDataWorkingDir(999991) + "/index/elasticsearch_userdata_TC_150MB.tc");
        Assert.assertTrue("Local copy of the TC data container should exist", fTC.exists());

        File fYML = new File(IndexManager.getUserDataWorkingDir(999991) + "/index/elasticsearch.config.user" + 999991
                + ".yml");
        Assert.assertTrue("User specific ES YML file should exist", fYML.exists());
    }

    @Test
    public void testFailForMissingConfig() throws IndexManagerCoreException {
        this.exception.expect(IndexManagerCoreException.class);
        this.indexManager.getESTransportClient(999992);
    }

    @Test
    public void testRetrieveTransportClientAndClusterState() throws IndexManagerCoreException {
        //startup or get running instance
        Client client = this.indexManager.initAndCreateAndDoEverthing(999992L);
        assertNotNull(client);
        ClusterState state = this.indexManager.getESClusterState(999992L);
        assertNotNull(state);
        assertEquals(new ClusterName("user999992"), state.getClusterName());
        client.close();
    }

    @Test
    public void testConnectViaTransportClient() throws IndexManagerCoreException, IOException {

        this.indexManager.startupInstance(999992);

        // check instance up and running
        RunningIndexUserConfig conf = this.indexManager.getRunningIndexUserConfig(999992);
        int httpPort = conf.getHttpPort();
        String drive = conf.getMountedTCDriveLetter();
        Assert.assertNotNull("mounting TC data drive for user should not fail", drive);
        Assert.assertTrue("ES http portnumber should have been assigned", httpPort > -1);
        System.out.println("user 999992 on port: " + httpPort + " and TC volume: " + drive);

        Assert.assertTrue("ES Instance is not running ",
                ESConfigurationHandler.isElasticSearchInstanceRunning(conf.getHostAddress(), httpPort));

        Settings settings = ImmutableSettings.settingsBuilder().put("cluster.name", "user" + 999992).build();

        // now try to connect with the TransportClient - requires the transport.tcp.port for connection
        Client client = new TransportClient(settings).addTransportAddress(new InetSocketTransportAddress(conf
                .getHostAddress().getHost(), conf.getTcpPort()));

        IndexResponse response = null;

        response = client
                .prepareIndex("twitter", "tweet", "1")
                .setSource(
                        XContentFactory.jsonBuilder().startObject().field("user", "john").field("postDate", new Date())
                                .field("message", "who dont it work").endObject()).execute().actionGet();

        Assert.assertTrue("Contains Index", response.getIndex().equals("twitter"));

        Assert.assertTrue("Contains Type", response.getType().equals("tweet"));

        DeleteResponse delresponse = client.prepareDelete("twitter", "tweet", "1").execute().actionGet();
        System.out.println("Deleted Index: " + delresponse.getIndex());
        Assert.assertTrue(delresponse.getIndex().equals("twitter"));
        client.close();

    }

    @Test
    public void testCreateIndexElementViaHttpClient() throws IndexManagerCoreException, IOException {

        this.indexManager.startupInstance(999991);
        System.out.println("startup done");

        RunningIndexUserConfig conf = this.indexManager.getRunningIndexUserConfig(999991);

        int httpPort = conf.getHttpPort();
        URL host = conf.getHostAddress();
        Assert.assertTrue("ES instance up and running?",
                ESConfigurationHandler.isElasticSearchInstanceRunning(host, httpPort));

        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            HttpPost postRequest = new HttpPost("http://" + conf.getHostAddress().getHost() + ":" + httpPort
                    + "/dummytestindex/article");

            StringEntity input = new StringEntity("{\"name\":\"ES JAVA API WorkAround\",\"category\":\"Garbage\"}");
            input.setContentType("application/json");
            postRequest.setEntity(input);

            try (CloseableHttpResponse response = httpClient.execute(postRequest)) {

                System.out.println(response.toString());
                if (response.getStatusLine().getStatusCode() != 201) {
                    Assert.fail("Failed : HTTP error code : " + response.getStatusLine().getStatusCode());
                }

                BufferedReader br = new BufferedReader(new InputStreamReader((response.getEntity().getContent())));

                String output;
                while ((output = br.readLine()) != null) {
                    System.out.println("got response: " + output);
                    Assert.assertTrue("Contains Index", output.contains("\"" + "_index" + "\"" + ":" + "\""
                            + "dummytestindex" + "\""));

                    Assert.assertTrue("Contains Type", output.contains("\"" + "_type" + "\"" + ":" + "\"" + "article"
                            + "\""));

                    Assert.assertTrue("Contains Created", output.contains("\"" + "created" + "\"" + ":" + "true"));
                }
            }
        }
    }

}
