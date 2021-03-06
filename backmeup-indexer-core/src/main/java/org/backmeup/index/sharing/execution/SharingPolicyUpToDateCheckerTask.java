package org.backmeup.index.sharing.execution;

import java.io.IOException;
import java.util.ArrayList;
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
import org.backmeup.index.storage.ThemisDataSink;
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
    @RunRequestScoped
    public void run() {
        checkExistingPolicies();
    }

    /**
     * Checks on actual deltas between policies and UUIDs to import and if found some redistributes IndexDocument data
     * to sharing partners
     */
    private void checkExistingPolicies() {
        checkDistributeIndexFragmentsToSharingPartners();
        checkImportSharingsForCurrentUsers();
        checkDeletionSharingsForCurrentUsers();
    }

    /**
     * We pre-distribute index-fragments to all Sharingpartners for policies (inkl. the ones that not yet been approved
     * or waiting for timespan to start. Allowing users to import data right after they approve an incoming sharing
     * policy (2-way instead of 3-way handshake). Distribution is done for SharingPolicies and HeritagePolicies
     */
    private void checkDistributeIndexFragmentsToSharingPartners() {

        //get the list of active users
        for (User activeUser : this.activeUsers.getActiveUsers()) {
            //iterate over all policies for this given user
            List<SharingPolicy> policies = new ArrayList<SharingPolicy>();
            policies.addAll(this.manager.getAllHeritagePoliciesOwnedByUser(activeUser));
            policies.addAll(this.manager.getAllWaiting4HandshakeAndScheduledAndActivePoliciesOwnedByUser(activeUser));

            //distribute serealized index fragments contained in the policies
            for (SharingPolicy policy : policies) {
                List<UUID> missingImports = this.pol2uuidConverter.getMissingDeltaToImportForSharingPartner(policy);
                //TODO AL: currently we distribute the doc over and over again until it's imported by the sharing partner 
                //e.g. hold a db table of what we already distributed to avoid noise
                this.log.debug("found a delta of: " + missingImports.size() + " missing elements for policy: " + policy.getId()
                        + " which have not yet been distributed and imported by the sharing partner");
                //iterate over missing elements
                for (UUID missingUUID : missingImports) {
                    //fetch document from document owner's encrypted storage - it's accessible as he's the active user
                    IndexDocument doc;
                    try {
                        doc = ThemisEncryptedPartition.getIndexFragment(missingUUID, activeUser, IndexFragmentType.IMPORTED_USER_OWNED,
                                this.activeUsers.getMountedDrive(activeUser));

                        //add additional entries for sharing within the IndexDocument
                        doc.field(IndexFields.FIELD_SHARED_BY_USER_ID, activeUser.id());
                        //active user is always the document owner - reset the flag
                        doc.field(IndexFields.FIELD_OWNER_ID, policy.getWithUserID());

                        //check the different sharing policies and create according import tasks for doc
                        this.policyExecution.distributeIndexFragmentToSharingParnter(policy, doc);

                    } catch (Exception e) {
                        this.log.debug("failed to distribute item: " + missingUUID + " for policy:" + policy.toString(), e);
                    }
                }
            }
        }
    }

    /**
     * Check on all incoming sharing policies for active users, fetch the pre-distributed indexfragments and create
     * to_import statements.
     */
    private void checkImportSharingsForCurrentUsers() {
        //get the list of active users
        for (User activeUser : this.activeUsers.getActiveUsers()) {
            //iterate over all policies for this given user
            for (SharingPolicy policy : this.manager.getAllActivePoliciesSharedWithUser(activeUser)) {
                List<UUID> missingImports = this.pol2uuidConverter.getMissingDeltaToImportForSharingPartner(policy);
                this.log.debug("found a delta of: " + missingImports.size() + " missing elements for policy: " + policy.toString()
                        + " to import");
                //iterate over missing elements
                for (UUID missingUUID : missingImports) {
                    //fetch document from document owner's encrypted storage - it's accessible as he's the active user
                    IndexDocument doc;
                    try {
                        //fetch the pre-distributed indexfragment from the user's public drop-off place
                        doc = ThemisDataSink.getIndexFragment(missingUUID, activeUser,
                                org.backmeup.index.storage.ThemisDataSink.IndexFragmentType.TO_IMPORT_SHARED_WITH_USER,
                                this.activeUsers.getPrivateKey(activeUser.id()));

                        //check the different sharing policies and create according import tasks for doc
                        this.policyExecution.executeImportSharingParnter(policy, doc);

                    } catch (IOException e) {
                        this.log.debug("failed to execute import for item: " + missingUUID + " for policy:" + policy.toString(), e);
                    }
                }
            }

        }
    }

    private void checkDeletionSharingsForCurrentUsers() {
        //get the list of active users
        for (User activeUser : this.activeUsers.getActiveUsers()) {
            //iterate over all policies for this given user
            for (SharingPolicy policy : this.manager.getAllWaitingForDeletionPoliciesSharedWithUser(activeUser)) {
                List<UUID> missingDeletions = this.pol2uuidConverter.getMissingDeltaToDeleteForSharingPartner(policy);
                this.log.debug("found a delta of: " + missingDeletions.size() + " missing deletions for policy: " + policy.toString());
                //iterate over missing elements required for deletion for this policy
                for (UUID missingUUID : missingDeletions) {
                    try {
                        //check the different sharing policies and create according deletion tasks
                        this.policyExecution.executeDeletionSharingParnter(policy, missingUUID);

                    } catch (IOException e) {
                        this.log.debug("failed to execute item: " + missingUUID + " for policy:" + policy.toString(), e);
                    }
                }
                //once all items are marked for deletion: delete the policy itself
                this.manager.markPolicyAsDeleted(policy);
            }
        }
    }

}
