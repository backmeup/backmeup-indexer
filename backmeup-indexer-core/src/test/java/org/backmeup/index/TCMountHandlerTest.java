package org.backmeup.index;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang.SystemUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

public class TCMountHandlerTest {

    // The Password for the TestTCVol1.tc file is 12345

    @Before
    @After
    public void unmountAllDrives() {
        try {
            TCMountHandler.unmountAll();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    @Test
    public void truecryptAvailable() {
        // how to get the current working dir:
        // System.out.println(System.getProperty("user.dir"));
        boolean b = TCMountHandler.checkTrueCryptAvailable();
        Assert.assertEquals("check TrueCrypt available: TC not available or properly configured on the system", true, b);
    }

    @Test
    public void mountAndUnmountTestContainerWindows() {
        Assume.assumeTrue(SystemUtils.IS_OS_WINDOWS);
        File tcTestFile = new File("src/main/resources/tests/TestTCVol1.tc");
        try {
            String drive = TCMountHandler.mount(tcTestFile, "12345", "I");
            Assert.assertEquals("TrueCrypt Testvolume did not get mounted", true, TCMountHandler.isDriveMounted("I"));
            TCMountHandler.unmount(drive);
            Assert.assertEquals("TrueCrypt Testvolume did not get unmounted properly", false,
                    TCMountHandler.isDriveMounted(drive));
        } catch (Exception e) {
            System.out.println(e);
            // should not happen in this test case
            Assert.fail("should never fail mounting this scenario");
        }
    }

    @Test
    public void mountAndUnmountTestContainerLinux() {
        Assume.assumeTrue(SystemUtils.IS_OS_LINUX);
        File tcTestFile = new File("src/main/resources/tests/TestTCVol1.tc");
        try {
            String mountingPoint = TCMountHandler.mount(tcTestFile, "12345", "/media/themis/volume0");
            System.out.println(mountingPoint);

            Assert.assertEquals("TrueCrypt Testvolume did not get mounted", true,
                    TCMountHandler.isDriveMounted(mountingPoint));

            TCMountHandler.unmount(mountingPoint);
            Assert.assertEquals("TrueCrypt Testvolume did not get unmounted properly", false,
                    TCMountHandler.isDriveMounted(mountingPoint));

        } catch (Exception e) {
            System.out.println(e.toString());
            // should not happen in this test case
            Assert.fail("should never fail mounting this scenario." + e.toString());
        }
    }

    @Test
    public void mountVolumeTwiceAtSameTimeLinux() {
        Assume.assumeTrue(SystemUtils.IS_OS_LINUX);
        // mounting the same TrueCrypt Volume file twice at the same time is not
        // possible
        File tcTestFile = new File("src/main/resources/tests/TestTCVol1.tc");
        try {
            String mountingPoint = TCMountHandler.mount(tcTestFile, "12345", "/media/themis/volume0");

            Assert.assertEquals("TrueCrypt Testvolume did not get mounted", true,
                    TCMountHandler.isDriveMounted(mountingPoint));

            TCMountHandler.mount(tcTestFile, "12345", mountingPoint);
            Assert.fail("IOException should have been thronw. TestFile " + tcTestFile.getAbsolutePath());

        } catch (IOException e) {
            // in this case we should have gotten an IOException and no other
            Assert.assertTrue(true);
        } catch (Exception e) {
            // should not happen in this test case
            Assert.fail("should never fail mounting this scenario");
        }
    }

    @Test
    public void mountVolumeTwiceAtSameTimeWindows() {
        Assume.assumeTrue(SystemUtils.IS_OS_WINDOWS);
        // mounting the same TrueCrypt Volume file twice at the same time is not
        // possible
        File tcTestFile = new File("src/main/resources/tests/TestTCVol1.tc");
        try {
            String drive1 = "I";
            drive1 = TCMountHandler.mount(tcTestFile, "12345", drive1);
            Assert.assertEquals("TrueCrypt Testvolume did not get mounted", true, TCMountHandler.isDriveMounted(drive1));

            String drive2 = "J";
            TCMountHandler.mount(tcTestFile, "12345", drive2);
            Assert.fail("IOException should have been thrown. TestFile " + tcTestFile.getAbsolutePath());

        } catch (IOException e) {
            // in this case we should have gotten an IOException and no other
            Assert.assertTrue(true);
        } catch (Exception e) {
            // should not happen in this test case
            Assert.fail("should never fail mounting this scenario");
        }
    }

    @Test
    public void mountTwiceOnSameDrive() {
        Assume.assumeTrue(SystemUtils.IS_OS_WINDOWS);
        File tcTestFile = new File("src/main/resources/tests/TestTCVol1.tc");
        File tcTestFile2 = new File("src/main/resources/tests/TestTCVol2.tc");
        try {
            String drive1 = "I";
            drive1 = TCMountHandler.mount(tcTestFile, "12345", drive1);
            Assert.assertEquals("TrueCrypt Testvolume did not get mounted", true, TCMountHandler.isDriveMounted(drive1));

            String drive2 = TCMountHandler.mount(tcTestFile2, "12345", drive1);
            Assert.assertFalse("Volume can't get mounted twice on the same drive", drive1.equals(drive2));
            Assert.assertEquals("TrueCrypt Testvolume did not get mounted", true, TCMountHandler.isDriveMounted(drive2));
        } catch (Exception e) {
            // should not happen in this test case
            Assert.fail("should never fail mounting this scenario");
        }
    }

    @Test
    public void unmountAllDrivesTest() throws Exception {
        Assume.assumeTrue(SystemUtils.IS_OS_WINDOWS);
        File tcTestFile = new File("src/main/resources/tests/TestTCVol1.tc");
        File tcTestFile2 = new File("src/main/resources/tests/TestTCVol2.tc");

        String d1 = "I";
        d1 = TCMountHandler.mount(tcTestFile, "12345", d1);
        Assert.assertTrue("TrueCrypt Testvolume did not get mounted", TCMountHandler.isDriveMounted(d1));

        String d2 = "J";
        d2 = TCMountHandler.mount(tcTestFile2, "12345", d2);
        Assert.assertTrue("TrueCrypt Testvolume did not get mounted", TCMountHandler.isDriveMounted(d2));

        TCMountHandler.unmountAll();
        Assert.assertFalse("TrueCrypt Testvolume did not get mounted", TCMountHandler.isDriveMounted(d1));
        Assert.assertFalse("TrueCrypt Testvolume did not get mounted", TCMountHandler.isDriveMounted(d2));

    }

}
