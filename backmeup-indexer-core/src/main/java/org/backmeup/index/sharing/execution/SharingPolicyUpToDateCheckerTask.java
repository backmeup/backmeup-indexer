package org.backmeup.index.sharing.execution;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.backmeup.index.ActiveUsers;
import org.backmeup.index.api.IndexFields;
import org.backmeup.index.model.IndexDocument;
import org.backmeup.index.model.User;
import org.backmeup.index.sharing.policy.SharingPolicy;
import org.backmeup.index.sharing.policy.SharingPolicy2DocumentUUIDConverter;
import org.backmeup.index.sharing.policy.SharingPolicyManager;
import org.backmeup.index.storage.ThemisEncryptedPartition;
import org.backmeup.index.storage.ThemisEncryptedPartition.IndexFragmentType;
import org.backmeup.index.utils.cdi.RunRequestScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Takes care of querying existing policies and checks if they are still up to date (content wise) and if not triggers
 * triggers the content import/export process
 *
 */
@ApplicationScoped
public class SharingPolicyUpToDateCheckerTask {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ScheduledExecutorService exec = Executors.newScheduledThreadPool(1);
    private int SECONDS_BETWEEN_RECHECKING = 120;

    @Inject
    private ActiveUsers activeUsers;
    @Inject
    private SharingPolicy2DocumentUUIDConverter pol2uuidConverter;
    @Inject
    private SharingPolicyExecution policyExecution;
    @Inject
    private SharingPolicyManager manager = SharingPolicyManager.getInstance(); //TODO need to add bean and init methods in lifecycle

    @RunRequestScoped
    public void startupSharingPolicyExecution() {
        startPolicyExecution();
        this.log.debug("startup() SharingPolicyUpToDateCheckerTask (ApplicationScoped) completed");
    }

    @RunRequestScoped
    public void shutdownSharingPolicyExecution() {
        stopPolicyExecution();
        this.log.debug("shutdown() SharingPolicyUpToDateCheckerTask (ApplicationScoped) completed");
    }

    private void startPolicyExecution() {

        this.exec.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                checkExistingPolicies();
            }
        }, this.SECONDS_BETWEEN_RECHECKING, this.SECONDS_BETWEEN_RECHECKING, java.util.concurrent.TimeUnit.SECONDS);
    }

    public void stopPolicyExecution() {
        this.log.debug("SharingPolicyExecutionTask stopping distribution thread");
        this.exec.shutdownNow();
    }

    /**
     * Checks on actual deltas between policies and UUIDs to import and if found some redistributes IndexDocument data
     * to sharing partners
     */
    private void checkExistingPolicies() {
        checkImportSharings();
        checkDeletionSharings();
    }

    private void checkImportSharings() {
        //get the list of active users
        for (User activeUser : this.activeUsers.getActiveUsers()) {
            //iterate over all policies for this given user
            for (SharingPolicy policy : this.manager.getAllActivePoliciesForUser(activeUser)) {
                //TODO add a threshold on policy.getPolicyLastCheckedDate(), e.g. only recheck every 5 Minutes
                List<UUID> missingImports = this.pol2uuidConverter.getMissingDeltaToImportForSharingPartner(policy);
                this.log.debug("found a delta of: " + missingImports.size() + " missing imports for policy: "
                        + policy.toString());
                //iterate over missing elements
                for (UUID missingUUID : missingImports) {
                    //fetch document from document owner's encrypted storage - it's accessible as he's the active user
                    IndexDocument doc;
                    try {
                        doc = ThemisEncryptedPartition.getIndexFragment(missingUUID, activeUser,
                                IndexFragmentType.IMPORTED_USER_OWNED, this.activeUsers.getMountedDrive(activeUser));

                        //add additional entries for sharing within the IndexDocument
                        doc.field(IndexFields.FIELD_SHARED_BY_USER_ID, activeUser.id());
                        //active user is always the document owner - reset the flag
                        doc.field(IndexFields.FIELD_OWNER_ID, policy.getWithUserID());

                        //check the different sharing policies and create according import tasks for doc
                        this.policyExecution.executeImportSharingParnter(policy, doc);

                    } catch (IOException e) {
                        this.log.debug("failed to execute item: " + missingUUID + " for policy:" + policy.toString(), e);
                    }
                }
            }

        }
    }

    private void checkDeletionSharings() {
        //TODO Need to implement
    }

    /**
     * Used for JUnit Tests to modify the default value
     * 
     * @param seconds
     */
    protected void setFrequency(int seconds) {
        this.SECONDS_BETWEEN_RECHECKING = seconds;
    }

}
