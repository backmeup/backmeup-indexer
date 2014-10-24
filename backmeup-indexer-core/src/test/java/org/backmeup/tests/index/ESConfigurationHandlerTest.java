package org.backmeup.tests.index;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;

import org.backmeup.index.ESConfigurationHandler;
import org.backmeup.index.config.Configuration;
import org.backmeup.index.utils.file.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ESConfigurationHandlerTest {

	URL host;

	@Before
	public void before() {
		try {
			this.host = new URL("http://localhost");
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testESHomeDirSet() {
		String eshome = Configuration.getProperty("elasticsearch.home.dir");
		Assert.assertNotNull(eshome);
	}

	@Test
	public void testElasticSearchExecutableAvailable() {
		boolean b = ESConfigurationHandler.checkElasticSearchAvailable();
		Assert.assertEquals(
				"check ElasticSearch available: ES not available or properly configured on the system",
				true, b);
	}

	@Test
	public void testNonSupportedPortRange() {
		try {
			ESConfigurationHandler.createUserYMLStartupFile(100, this.host,
					9210, 9310, null);
			Assert.fail("This code block should not be reached");
		} catch (NumberFormatException e) {
			Assert.assertTrue(
					"Correct exception, number not within the accepted port range",
					true);
		} catch (Exception e) {
			Assert.fail("This code block should not be reached");
		}
	}

	@Test
	public void testReplaceTokens() {
		try {
			File f = ESConfigurationHandler.createUserYMLStartupFile(100,
					this.host, 9310, 9210, null);

			boolean bClusterName = false;
			boolean bTCPPort = false;
			boolean bHTTPPort = false;

			Reader in = new FileReader(f);
			BufferedReader br = new BufferedReader(in);

			String line;
			while ((line = br.readLine()) != null) {
				if (line.contains("cluster.name: user" + 100)) {
					bClusterName = true;
				}
				if (line.contains("transport.tcp.port: 9310")) {
					bTCPPort = true;
				}
				if (line.contains("http.port: 9210")) {
					bHTTPPort = true;
				}
			}
			in.close();

			// check if the new user specific configuration file was properly
			// written
			Assert.assertEquals("Clustername Config missing", true,
					bClusterName);
			Assert.assertEquals("TCP Port Config missing", true, bTCPPort);
			Assert.assertEquals("HTTPPort Config missing", true, bHTTPPort);

			FileUtils.deleteDirectory(f.getParentFile().getParentFile());

		} catch (ExceptionInInitializerError e) {
			Assert.fail("Should never happen for a properly configured instance "
					+ e);
		} catch (IOException e) {
			Assert.fail("Should never happen for a properly configured instance "
					+ e);
		}
	}
}
