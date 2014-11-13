package org.backmeup.index;

import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Contains tests for sharing index fragments with other users - Includes the physical data operations (copying
 * IndexDocuments, etc) - Importing and deleting fragments from Elasticsearch itself - rebuilding an ES index from
 * scratch (file basis)
 */
@RunWith(JUnit4.class)
public class IndexFragmentSharingTests extends IndexManagerSetup {

    // TODO Add Startup Class for all Integration Tests
    // TODO @Before -> Put IndexFragment to-import
    // TODO activateverifyIndexBuiltProperlyFromFragments

    @Rule
    public ExpectedException exception = ExpectedException.none();

    /* private RunningIndexUserConfig startupInstance(int userID) throws IndexManagerCoreException, IOException {

         this.indexManager.startupInstance(userID);
         RunningIndexUserConfig conf = this.indexManager.getRunningIndexUserConfig(userID);

         int httpPort = conf.getHttpPort();
         String drive = conf.getMountedTCDriveLetter();

         System.out.println("user " + userID + " on port: " + httpPort + " and TC volume: " + drive);
         // check instance up and running
         ESConfigurationHandler.isElasticSearchInstanceRunning(conf.getHostAddress(), httpPort);

         return conf;
     }*/

    /*
     * @Test
     * 
     * @Ignore public void verifyIndexBuiltProperlyFromFragments() {
     * System.out.println("Verifying indexing content"); Client client =
     * node.client(); SearchResponse response =
     * rawClient.prepareSearch("backmeup")
     * .setQuery(QueryBuilders.matchAllQuery()).execute().actionGet();
     * SearchHits hits = response.getHits(); Assert.assertEquals(3,
     * hits.getHits().length);
     * 
     * for (SearchHit hit : response.getHits().getHits()) { Map<String, Object>
     * source = hit.getSource(); for (String key : source.keySet()) {
     * System.out.println(key + ": " + source.get(key));
     * 
     * if (key.equals("owner_name")) Assert.assertEquals("john.doe",
     * source.get(key));
     * 
     * if (key.equals("owner_id")) Assert.assertEquals(1, source.get(key));
     * 
     * if (key.equals("backup_sources"))
     * Assert.assertEquals("org.backmeup.dummy", source.get(key));
     * 
     * if (key.equals("indexrecord_uuid"))
     * Assert.assertEquals("NEED TO ADD UUID HERE", source.get(key)); } }
     * 
     * System.out.println("Done."); }
     */

}
