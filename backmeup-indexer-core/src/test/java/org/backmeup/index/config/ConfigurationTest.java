package org.backmeup.index.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Test;

public class ConfigurationTest {

    @Test
    public void testGetNonExistingProperty() {
        String ret = Configuration.getProperty("bogus key");
        assertEquals(null, ret);
    }

    @Test
    public void testGetNonExistingPropertyAsList() {
        List<String> ret = Configuration.getPropertyList("bogus key");
        assertNotNull(ret);
        assertEquals(true, ret.isEmpty());

        ret = Configuration.getPropertyList(null);
        assertNotNull(ret);
        assertEquals(true, ret.isEmpty());
    }

    @Test
    public void testGetTrueCryptMountableDrivesAsList() {
        List<String> ret = Configuration.getPropertyList("truecrypt.mountable.drives");
        assertNotNull(ret);
        assertEquals(false, ret.isEmpty());
    }

}
