package org.backmeup.tests.index;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import junit.framework.Assert;

import org.backmeup.index.IndexManager;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

public class IndexManagerTest {

	@After
	public void after() {
		// TODO wieder reaktivieren, wenn instance up and running geprüft wurde
		// IndexManager.getInstance().shutdown(999991);
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
}
