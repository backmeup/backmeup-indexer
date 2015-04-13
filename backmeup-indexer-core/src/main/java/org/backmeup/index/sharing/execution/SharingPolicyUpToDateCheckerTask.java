package org.backmeup.index.sharing.execution;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Takes care of querying existing policies and checks if they are still up to date (content wise) and if not triggers
 * triggers the content import/export process
 *
 */
@ApplicationScoped
public class SharingPolicyUpToDateCheckerTask implements Runnable {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    private ActiveUsers activeUsers;
    @Inject
    private SharingPolicy2DocumentUUIDConverter pol2uuidConverter;
    @Inject
    private SharingPolicyExecution policyExecution;
    @Inject
    private SharingPolicyManager manager;

    @Override
    public void run() {
        checkExistingPolicies();
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
            for (SharingPolicy policy : this.manager.getAllActivePoliciesOwnedByUser(activeUser)) {
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

}
