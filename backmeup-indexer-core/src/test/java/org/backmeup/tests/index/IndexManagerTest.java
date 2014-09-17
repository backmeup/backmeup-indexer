package org.backmeup.tests.index;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.backmeup.index.ESConfigurationHandler;
import org.backmeup.index.IndexManager;
import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class IndexManagerTest {

	@After
	public void after() {
		// TODO JUST FOR TESTING - reactivate later
		// IndexManager.getInstance().shutdown(999991);
		// IndexManager.getInstance().shutdown(999992);

	}

	@AfterClass
	public static void cleanup() {
		// ESConfigurationHandler.stopAll();
	}

	@Test
	@Ignore
	public void testESandTCLaunchTest() {
		IndexManager indexManager = IndexManager.getInstance();
		try {
			indexManager.startup(999992);
			int httpPort = indexManager.getESTHttpPort(999992);
			String drive = indexManager.getTCMountedVolume(999992);
			Assert.assertNotNull(
					"mounting TC data drive for user should not fail", drive);
			Assert.assertTrue("ES http portnumber should have been assigned",
					httpPort > -1);
			System.out.println("user 999992 on port: " + httpPort
					+ " and TC volume: " + drive);
			// TODO check instance up and running
		} catch (ExceptionInInitializerError | IllegalArgumentException
				| IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail("Should not fail when properly configured" + e);
		}
		// TODO need to check if the instance is up and running
		fail("Not yet implemented");
	}

	@Test
	@Ignore
	public void testUserStartupArtefakts() {
		IndexManager indexManager = IndexManager.getInstance();
		try {
			indexManager.startup(999991);
			File fTC = new File(IndexManager.getUserDataWorkingDir(999991)
					+ "/index/elasticsearch_userdata_TC_150MB.tc");
			Assert.assertTrue(
					"Local copy of the TC data container should exist",
					fTC.exists());

			File fYML = new File(IndexManager.getUserDataWorkingDir(999991)
					+ "/index/elasticsearch.config.user" + 999991 + ".yml");
			Assert.assertTrue("User specific ES YML file should exist",
					fYML.exists());

		} catch (IOException | ExceptionInInitializerError
				| IllegalArgumentException | InterruptedException e) {
			fail("Should not reach this code block" + e);
		}

	}

	@Test
	@Ignore
	public void testUserShutdownArtefakts() {
		fail("Not yet implemented");
	}

	@Test
	@Ignore
	public void testConnectViaTransportClient() {

		IndexManager indexManager = IndexManager.getInstance();
		try {
			indexManager.startup(999991);
		} catch (ExceptionInInitializerError | IllegalArgumentException
				| IOException | InterruptedException e1) {
			fail("Should not reach this code block" + e1);
		}

		int httpPort = IndexManager.getInstance().getESTHttpPort(999991);
		int tcpPort = IndexManager.getInstance().getESTcpPort(999991);
		Assert.assertTrue("No valid http port for user 999991 assigned. Port: "
				+ httpPort, httpPort > -1);

		Settings settings = ImmutableSettings.settingsBuilder()
				.put("cluster.name", "user" + 999991)
				.put("client.transport.sniff", true).build();
		Client client = new TransportClient(settings)
				.addTransportAddress(new InetSocketTransportAddress(
						"localhost", httpPort));

		IndexResponse response = null;
		try {
			response = client
					.prepareIndex("twitter", "tweet", "1")
					.setSource(
							XContentFactory.jsonBuilder().startObject()
									.field("user", "john")
									.field("postDate", new Date())
									.field("message", "who dont it work")
									.endObject()).execute().actionGet();

			Assert.assertTrue("Contains Index",
					response.getIndex().equals("twitter"));

			Assert.assertTrue("Contains Type",
					response.getType().equals("tweet"));

		} catch (ElasticSearchException e) {
			e.printStackTrace();
			fail("Should not fail" + e);
		} catch (IOException e) {
			e.printStackTrace();
			fail("Should not fail" + e);
		}
	}

	@Test
	public void testCreateIndexElementViaHttpClient() {
		try {
			IndexManager indexManager = IndexManager.getInstance();
			try {
				indexManager.startup(999991);
				System.out.println("startup done");

			} catch (ExceptionInInitializerError | IllegalArgumentException
					| IOException | InterruptedException e1) {
				fail("Should not reach this code block" + e1);
			}

			int httpPort = IndexManager.getInstance().getESTHttpPort(999991);
			Assert.assertTrue("ES instance up and running?",
					ESConfigurationHandler
							.isElasticSearchInstanceRunning(httpPort));

			DefaultHttpClient httpClient = new DefaultHttpClient();

			// TODO set port dynamically!!
			HttpPost postRequest = new HttpPost("http://localhost:" + httpPort
					+ "/dummytestindex/article");

			StringEntity input = new StringEntity(
					"{\"name\":\"ES JAVA API WorkAround\",\"category\":\"Garbage\"}");
			input.setContentType("application/json");
			postRequest.setEntity(input);

			HttpResponse response = httpClient.execute(postRequest);

			System.out.println(response.toString());
			if (response.getStatusLine().getStatusCode() != 201) {
				Assert.fail("Failed : HTTP error code : "
						+ response.getStatusLine().getStatusCode());
			}

			BufferedReader br = new BufferedReader(new InputStreamReader(
					(response.getEntity().getContent())));

			String output;
			while ((output = br.readLine()) != null) {
				System.out.println("got response: " + output);
				Assert.assertTrue("Contains Index", output.contains("\""
						+ "_index" + "\"" + ":" + "\"" + "dummytestindex"
						+ "\""));

				Assert.assertTrue("Contains Type", output.contains("\""
						+ "_type" + "\"" + ":" + "\"" + "article" + "\""));

				Assert.assertTrue("Contains Created", output.contains("\""
						+ "created" + "\"" + ":" + "true"));
			}

			httpClient.getConnectionManager().shutdown();

		} catch (Exception e) {
			Assert.fail(e.toString());

		}
	}

	@Test
	public void testShutdown() {
		// test if the shutdown and isRunning implementation is properly working
		int userID = 999993;
		try {
			IndexManager indexManager = IndexManager.getInstance();

			indexManager.startup(userID);

			int httpPort = IndexManager.getInstance().getESTHttpPort(userID);
			Assert.assertTrue("ES instance up and running?",
					ESConfigurationHandler
							.isElasticSearchInstanceRunning(httpPort));

			indexManager.shutdown(userID);

			Assert.assertFalse("ES instance up and running?",
					ESConfigurationHandler
							.isElasticSearchInstanceRunning(httpPort));

		} catch (Exception e) {
			Assert.fail(e.toString());
		}

	}
}
