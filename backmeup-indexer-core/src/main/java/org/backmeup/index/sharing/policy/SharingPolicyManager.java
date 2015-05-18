package org.backmeup.index.sharing.policy;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.backmeup.index.dal.SharingPolicyDao;
import org.backmeup.index.model.User;
import org.backmeup.index.sharing.policy.SharingPolicy.ActivityState;
import org.backmeup.index.utils.cdi.RunRequestScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Allows to define sharing policies e.g. User A shares IndexDocument with User B, User A shares Backup with User B,
 * etc.
 *
 */
@ApplicationScoped
public class SharingPolicyManager {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    private SharingPolicyDao sharingPolicyDao;

    public SharingPolicyManager() {
    }

    @RunRequestScoped
    public void startupSharingPolicyManager() {
        this.log.debug("startup() SharingPolicyManager (ApplicationScoped) completed");
    }

    @RunRequestScoped
    public void shutdownSharingPolicyManager() {
        this.log.debug("shutdown() SharingPolicyManager (ApplicationScoped) completed");
    }

    public List<SharingPolicy> getAllActivePoliciesOwnedByUser(User user) {
        return this.sharingPolicyDao.getAllSharingPoliciesFromUserInState(user, ActivityState.ACCEPTED_AND_ACTIVE);
    }

    public List<SharingPolicy> getAllWaiting4HandshakeAndActivePoliciesOwnedByUser(User user) {
        return this.sharingPolicyDao.getAllSharingPoliciesFromUserInState(user, ActivityState.ACCEPTED_AND_ACTIVE,
                ActivityState.CREATED_AND_WAITING_FOR_HANDSHAKE);
    }

    public List<SharingPolicy> getAllWaiting4HandshakeAndActivePoliciesSharedWithUser(User user) {
        return this.sharingPolicyDao.getAllSharingPoliciesWithUserInState(user, ActivityState.ACCEPTED_AND_ACTIVE,
                ActivityState.CREATED_AND_WAITING_FOR_HANDSHAKE);
    }

    public List<SharingPolicy> getAllActivePoliciesBetweenUsers(User fromUser, User sharingP) {
        return this.sharingPolicyDao.getAllSharingPoliciesBetweenUserInState(fromUser, sharingP,
                ActivityState.ACCEPTED_AND_ACTIVE);
    }

    public List<SharingPolicy> getAllWaitingForDeletionPoliciesOwnedByUser(User user) {
        return this.sharingPolicyDao.getAllSharingPoliciesFromUserInState(user, ActivityState.WAITING_FOR_DELETION);
    }

    public List<SharingPolicy> getAllWaitingForDeletionPoliciesSharedWithUser(User user) {
        return this.sharingPolicyDao.getAllSharingPoliciesWithUserInState(user, ActivityState.WAITING_FOR_DELETION);
    }

    public SharingPolicy createAndAddSharingPolicy(User owner, User sharingWith, SharingPolicies policy) {
        return createAndAddSharingPolicy(owner, sharingWith, policy, null, null, null);
    }

    public SharingPolicy createAndAddSharingPolicy(User owner, User sharingWith, SharingPolicies policy, String name,
            String description) {
        return createAndAddSharingPolicy(owner, sharingWith, policy, null, name, description);
    }

    /**
     * 
     * @param owner
     * @param sharingWith
     * @param policy
     * @param sharedElementID
     *            either the IndexDocument UUID for SHARE_DOCUMENT or the BackupJobID for ShareBackupJob
     * @return
     */
    public SharingPolicy createAndAddSharingPolicy(User owner, User sharingWith, SharingPolicies policy,
            String sharedElementID, String name, String description) {
        SharingPolicy shPol = new SharingPolicy(owner, sharingWith, policy, sharedElementID, name, description);
        return addSharingPolicy(shPol);
    }

    /**
     * Does not filter out duplicates. The same policy can be added more than once
     * 
     * @param shPolicy
     * @return
     */
    public SharingPolicy addSharingPolicy(SharingPolicy shPolicy) {

        shPolicy.setState(ActivityState.CREATED_AND_WAITING_FOR_HANDSHAKE);

        shPolicy = this.sharingPolicyDao.save(shPolicy);
        this.log.debug("adding SharingPolicy " + shPolicy.toString());
        return shPolicy;
    }

    public void removeSharingPolicy(Long policyID) {
        SharingPolicy p = this.sharingPolicyDao.getByEntityId(policyID);
        removeSharingPolicy(p);
        this.log.debug("updated SharingPolicy for deletion: " + p.toString());
    }

    public void removeSharingPolicy(SharingPolicy p) {
        //just set the state, deletion from dao will be handled by the SharingPolicyUp2DateCheckerTask
        p.setState(ActivityState.WAITING_FOR_DELETION);
        this.sharingPolicyDao.merge(p);
    }

    public void removeAllSharingPoliciesForUser(User owner) {
        for (SharingPolicy policy : this.sharingPolicyDao.getAllSharingPoliciesFromUser(owner)) {
            this.removeSharingPolicy(policy);
        }
    }

    /**
     * Once all document removal operations took place the policy is set as deleted and will no longer be checked
     * 
     * @param p
     */
    public void markPolicyAsDeleted(SharingPolicy p) {
        p.setState(ActivityState.DELETED);
        this.sharingPolicyDao.merge(p);
    }

    /**
     * User accepts the incoming sharing he/she received
     * 
     * @param user
     *            the user which received the incoming sharing
     * @param policyID
     *            the sharingpolicy ID that contains the sharing for the user as sharedWith
     */
    public void approveIncomingSharing(User user, Long policyID) {
        SharingPolicy p = this.sharingPolicyDao.getAllSharingPoliciesWithUserAndPolicyID(user, policyID);
        if (p != null) {
            p.setState(ActivityState.ACCEPTED_AND_ACTIVE);
            this.sharingPolicyDao.merge(p);
            this.log.debug("approved incoming sharing for user: " + user.id() + " and SharingPolicy " + p.getId());
        } else {
            String s = "unable to approve incoming sharing for user: " + user.id() + " and SharingPolicy " + policyID
                    + " - policy does not exist";
            this.log.debug(s);
            throw new IllegalArgumentException(s);
        }
    }

    /**
     * User declines the incoming sharing he/she received
     * 
     * @param user
     *            the user which received the incoming sharing
     * @param policyID
     *            the sharingpolicy ID that contains the sharing for the user as sharedWith
     */
    public void declineIncomingSharing(User user, Long policyID) {
        SharingPolicy p = this.sharingPolicyDao.getAllSharingPoliciesWithUserAndPolicyID(user, policyID);
        if (p != null) {
            p.setState(ActivityState.WAITING_FOR_DELETION);
            this.sharingPolicyDao.merge(p);
            this.log.debug("declined incoming sharing for user: " + user.id() + " and SharingPolicy " + p.getId());
        } else {
            String s = "unable to decline incoming sharing for user: " + user.id() + " and SharingPolicy " + policyID
                    + " - policy does not exist";
            this.log.debug(s);
            throw new IllegalArgumentException(s);
        }
    }
}
