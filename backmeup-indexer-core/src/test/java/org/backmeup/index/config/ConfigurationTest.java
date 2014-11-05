package org.backmeup.index.config;

import java.util.List;

import org.junit.Assert;

import org.backmeup.index.config.Configuration;
import org.junit.Test;

public class ConfigurationTest {

	@Test
	public void testGetNonExistingProperty() {
		String ret = Configuration.getProperty("bogus key");
		Assert.assertEquals(null, ret);
	}

	@Test
	public void testGetNonExistingPropertyAsList() {
		List<String> ret = Configuration.getPropertyList("bogus key");
		Assert.assertNotNull(ret);
		Assert.assertEquals(true, ret.isEmpty());

		ret = Configuration.getPropertyList(null);
		Assert.assertNotNull(ret);
		Assert.assertEquals(true, ret.isEmpty());
	}

	@Test
	public void testGetTrueCryptMountableDrivesAsList() {
		List<String> ret = Configuration
				.getPropertyList("truecrypt.mountable.drives");
		Assert.assertNotNull(ret);
		Assert.assertEquals(false, ret.isEmpty());
	}

}
