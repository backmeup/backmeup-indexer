package org.backmeup.tests.data.dummy;

import java.io.File;
import java.io.IOException;

import org.backmeup.data.dummy.ThemisDataSink;
import org.backmeup.index.config.Configuration;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ThemisDataSinkTest {

	@After
	public void after() {
		try {
			ThemisDataSink.deleteIndexTrueCryptContainer(99998);
		} catch (IOException e) {
		}
	}

	@AfterClass
	public static void afterclass() {
		try {
			ThemisDataSink.deleteIndexTrueCryptContainer(99998);
		} catch (IOException e) {
		}
		try {
			ThemisDataSink.deleteIndexTrueCryptContainer(99999);
		} catch (IOException e) {
		}
	}

	@Before
	public void before() {
		tcTemplateFile = new File(
				"src/main/resources/elasticsearch_userdata_template_TC_150MB.tc");
		try {
			ThemisDataSink.saveIndexTrueCryptContainer(tcTemplateFile, 99998);
		} catch (IOException e) {
		}
	}

	public File tcTemplateFile;

	@Test
	public void testHomeDirSet() {
		String eshome = Configuration.getProperty("themis-datasink.home.dir");
		Assert.assertNotNull(eshome);
	}

	@Test
	public void testStoreIndexTCContainerFileForUser() {
		File tcTemplateFile = new File(
				"src/main/resources/elasticsearch_userdata_template_TC_150MB.tc");
		try {
			ThemisDataSink.saveIndexTrueCryptContainer(tcTemplateFile, 99999);
		} catch (IOException e) {
			Assert.fail("Should not reach this part of the testcase " + e);
		}
	}

	@Test
	public void testGetIndexTCContainerFileForUser() {

		File tcTemplateFile = new File(
				"src/main/resources/elasticsearch_userdata_template_TC_150MB.tc");
		try {
			ThemisDataSink.saveIndexTrueCryptContainer(tcTemplateFile, 99999);
		} catch (IOException e) {
			Assert.fail("Should not reach this part of the testcase " + e);
		}

	}

	@Test
	public void testDeleteIndexTCContainerFileForUser() {
		try {
			ThemisDataSink.deleteIndexTrueCryptContainer(99998);
		} catch (IOException e) {
			Assert.fail("Should not reach this part of the testcase ");
		}
		try {
			ThemisDataSink.getIndexTrueCryptContainer(99998);
			Assert.fail("Should not be able to fetch a index truecrypt container file for this user");
		} catch (IOException e) {
			Assert.assertTrue(
					"Index Truecrypt container file for user properly deleted",
					true);
		}
	}
}
