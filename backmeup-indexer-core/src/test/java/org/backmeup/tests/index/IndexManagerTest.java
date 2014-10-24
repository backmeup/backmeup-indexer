package org.backmeup.tests.index;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Date;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.backmeup.index.ESConfigurationHandler;
import org.backmeup.index.IndexManager;
import org.backmeup.index.db.RunningIndexUserConfig;
import org.elasticsearch.ElasticsearchException;
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
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class IndexManagerTest {

	private EntityManagerFactory entityManagerFactory;
	private EntityManager entityManager;
	private IndexManager indexManager;

	@After
	public void after() {
		// TODO JUST FOR TESTING - reactivate later
		// IndexManager.getInstance().shutdown(999991);
		// IndexManager.getInstance().shutdown(999992);

		closeEntityManager();
	}

	@Before
	public void before() {
		createEntityManager();
		this.indexManager = IndexManager.getInstance();
		this.indexManager.setEntityManager(this.entityManager);
	}

	@AfterClass
	public static void cleanup() {
		// ESConfigurationHandler.stopAll();
	}

	public void createEntityManager() {
		this.entityManagerFactory = Persistence.createEntityManagerFactory(
				"org.backmeup.index.jpa", overwrittenJPAProps());
		this.entityManager = this.entityManagerFactory.createEntityManager();
	}

	public Properties overwrittenJPAProps() {
		Properties overwrittenJPAProps = new Properties();

		overwrittenJPAProps.setProperty("javax.persistence.jdbc.driver",
				"org.apache.derby.jdbc.EmbeddedDriver");
		overwrittenJPAProps.setProperty("hibernate.connection.driver_class",
				"org.apache.derby.jdbc.EmbeddedDriver");
		overwrittenJPAProps.setProperty("javax.persistence.jdbc.url",
				"jdbc:derby:target/junit;create=true");
		overwrittenJPAProps.setProperty("hibernate.connection.url",
				"jdbc:derby:target/junit;create=true");
		overwrittenJPAProps.setProperty("hibernate.dialect",
				"org.hibernate.dialect.DerbyDialect");
		overwrittenJPAProps.setProperty("hibernate.hbm2ddl.auto", "create");

		return overwrittenJPAProps;
	}

	private void closeEntityManager() {
		this.entityManager.close();
		this.entityManagerFactory.close();
	}

	@Test
	public void testESandTCLaunchTest() {
		try {
			this.indexManager.startupInstance(999992);
			RunningIndexUserConfig conf = this.indexManager
					.getRunningIndexUserConfig(999992);

			int httpPort = conf.getHttpPort();
			String drive = conf.getMountedTCDriveLetter();
			Assert.assertNotNull(
					"mounting TC data drive for user should not fail", drive);
			Assert.assertTrue("ES http portnumber should have been assigned",
					httpPort > -1);
			System.out.println("user 999992 on port: " + httpPort
					+ " and TC volume: " + drive);
			// check instance up and running
			Assert.assertTrue(
					"ES Instance is not running ",
					ESConfigurationHandler.isElasticSearchInstanceRunning(
							conf.getHostAddress(), httpPort));
		} catch (ExceptionInInitializerError | IllegalArgumentException
				| IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail("Should not fail when properly configured" + e);
		} finally {
			try {
				this.indexManager.shutdownInstance(999992);
			} catch (IllegalArgumentException | ExceptionInInitializerError
					| IOException | InterruptedException e) {
				System.out
						.println("Error shutting down instance for userID 999992");
			}
		}
	}

	@Test
	@Ignore
	public void testUserStartupArtefakts() {
		IndexManager indexManager = IndexManager.getInstance();
		try {
			indexManager.startupInstance(999991);
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
			indexManager.startupInstance(999991);
		} catch (ExceptionInInitializerError | IllegalArgumentException
				| IOException | InterruptedException e1) {
			fail("Should not reach this code block" + e1);
		}

		RunningIndexUserConfig conf = this.indexManager
				.getRunningIndexUserConfig(999991);

		int httpPort = conf.getHttpPort();
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

		} catch (ElasticsearchException e) {
			e.printStackTrace();
			fail("Should not fail" + e);
		} catch (IOException e) {
			e.printStackTrace();
			fail("Should not fail" + e);
		}
	}

	@Test
	@Ignore
	public void testCreateIndexElementViaHttpClient() {
		try {
			IndexManager indexManager = IndexManager.getInstance();
			try {
				indexManager.startupInstance(999991);
				System.out.println("startup done");

			} catch (ExceptionInInitializerError | IllegalArgumentException
					| IOException | InterruptedException e1) {
				fail("Should not reach this code block" + e1);
			}

			RunningIndexUserConfig conf = this.indexManager
					.getRunningIndexUserConfig(999991);

			int httpPort = conf.getHttpPort();
			URL host = conf.getHostAddress();
			Assert.assertTrue("ES instance up and running?",
					ESConfigurationHandler.isElasticSearchInstanceRunning(host,
							httpPort));

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
	@Ignore
	public void testShutdown() {
		// test if the shutdown and isRunning implementation is properly working
		int userID = 999993;
		try {
			IndexManager indexManager = IndexManager.getInstance();

			indexManager.startupInstance(userID);

			RunningIndexUserConfig conf = this.indexManager
					.getRunningIndexUserConfig(999991);

			int httpPort = conf.getHttpPort();
			URL host = conf.getHostAddress();
			Assert.assertTrue("ES instance up and running?",
					ESConfigurationHandler.isElasticSearchInstanceRunning(host,
							httpPort));

			indexManager.shutdownInstance(userID);

			Assert.assertFalse("ES instance up and running?",
					ESConfigurationHandler.isElasticSearchInstanceRunning(host,
							httpPort));

		} catch (Exception e) {
			Assert.fail(e.toString());
		}

	}
}
