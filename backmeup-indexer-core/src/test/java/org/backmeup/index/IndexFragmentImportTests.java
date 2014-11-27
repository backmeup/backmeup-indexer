package org.backmeup.index;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class IndexFragmentImportTests extends IndexManagerSetup {

    //TODO Datei händisch im Workspace anlegen oder mit UnitTest hin kopieren 

    @Test
    @Ignore
    public void importUserOwnedIndexFragmentsInES() {
        /*this.indexManager.startupInstance(999991);
        System.out.println("startup done");

        RunningIndexUserConfig conf = this.indexManager.getRunningIndexUserConfig(999991);

        int httpPort = conf.getHttpPort();
        URL host = conf.getHostAddress();
        Assert.assertTrue("ES instance up and running?",
                ESConfigurationHandler.isElasticSearchInstanceRunning(host, httpPort));*/
    }

}
