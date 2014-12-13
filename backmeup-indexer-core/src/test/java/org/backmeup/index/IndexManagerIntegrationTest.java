package org.backmeup.index;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
import org.backmeup.index.model.User;
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

public class IndexManagerIntegrationTest extends IndexManagerSetup {

    private static final User _999991L = new User(999991L); // TODO what is special about the user? better name!
    private static final User _999992L = new User(999992L); // TODO what is special about the user? better name!
    
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void testESandTCLaunchTest() throws IndexManagerCoreException, IOException {
        this.indexManager.startupInstance(_999992L);

        RunningIndexUserConfig conf = this.dao.findConfigByUser(_999992L);
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
        this.indexManager.startupInstance(_999991L);
        File fTC = new File(UserDataWorkingDir.getDir(_999991L) + "/index/elasticsearch_userdata_TC_150MB.tc");
        Assert.assertTrue("Local copy of the TC data container should exist", fTC.exists());

        File fYML = new File(UserDataWorkingDir.getDir(_999991L) + "/index/elasticsearch.config.user" + _999991L
                + ".yml");
        Assert.assertTrue("User specific ES YML file should exist", fYML.exists());
    }

    @Test
    public void testFailForMissingConfig() throws IndexManagerCoreException {
        this.exception.expect(IndexManagerCoreException.class);
        this.es.getESTransportClient(_999992L);
    }

    @Test
    public void testRetrieveTransportClientAndClusterState() throws IndexManagerCoreException {
        //startup or get running instance
        try (Client client = this.indexManager.initAndCreateAndDoEverthing(_999992L)) {
            assertNotNull(client);
            ClusterState state = this.es.getESClusterState(_999992L);
            assertNotNull(state);
            assertEquals(new ClusterName("user999992"), state.getClusterName());
        }
    }

    @Test
    public void testConnectViaTransportClient() throws IndexManagerCoreException, IOException {

        this.indexManager.startupInstance(_999992L);

        // check instance up and running
        RunningIndexUserConfig conf = this.dao.findConfigByUser(_999992L);
        int httpPort = conf.getHttpPort();
        String drive = conf.getMountedTCDriveLetter();
        assertNotNull("mounting TC data drive for user should not fail", drive);
        assertTrue("ES http portnumber should have been assigned", httpPort > -1);
        System.out.println("user 999992 on port: " + httpPort + " and TC volume: " + drive);

        assertTrue("ES Instance is not running ",
                ESConfigurationHandler.isElasticSearchInstanceRunning(conf.getHostAddress(), httpPort));

        Settings settings = ImmutableSettings.settingsBuilder().put("cluster.name", "user" + 999992).build();

        // now try to connect with the TransportClient - requires the transport.tcp.port for connection
        try (Client client = new TransportClient(settings).addTransportAddress(new InetSocketTransportAddress(conf
                .getHostAddress().getHost(), conf.getTcpPort()))) {
            IndexResponse response = null;
            
            response = client
                    .prepareIndex("twitter", "tweet", "1")
                    .setSource(
                            XContentFactory.jsonBuilder().startObject().field("user", "john").field("postDate", new Date())
                            .field("message", "who dont it work").endObject()).execute().actionGet();
            
            assertTrue("Contains Index", response.getIndex().equals("twitter"));
            
            assertTrue("Contains Type", response.getType().equals("tweet"));
            
            DeleteResponse delresponse = client.prepareDelete("twitter", "tweet", "1").execute().actionGet();
            System.out.println("Deleted Index: " + delresponse.getIndex());
            assertTrue(delresponse.getIndex().equals("twitter"));
        }
    }

    @Test
    public void testCreateIndexElementViaHttpClient() throws IndexManagerCoreException, IOException {

        this.indexManager.startupInstance(_999991L);
        System.out.println("startup done");

        RunningIndexUserConfig conf = this.dao.findConfigByUser(_999991L);

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
