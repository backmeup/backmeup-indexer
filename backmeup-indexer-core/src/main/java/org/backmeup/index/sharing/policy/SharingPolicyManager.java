package org.backmeup.index.sharing.policy;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.backmeup.index.dal.SharingPolicyDao;
import org.backmeup.index.model.User;
import org.backmeup.index.sharing.policy.SharingPolicy.ActivityState;
import org.backmeup.index.tagging.TaggedCollection;
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

    /**
     * Returns a list of all Active and Waiting4Handshake sharing policies owned by a given user which define the
     * Sharing of the provided tagged collection.
     * 
     * @param user
     * @param t
     * @return
     */
    public List<SharingPolicy> getAllWaiting4HandshakeAndActivePoliciesOwnedByUserContainingTaggedCollection(User user,
            TaggedCollection t) {
        List<SharingPolicy> activePolicies = this.sharingPolicyDao.getAllSharingPoliciesFromUserInState(user,
                ActivityState.ACCEPTED_AND_ACTIVE, ActivityState.CREATED_AND_WAITING_FOR_HANDSHAKE);
        return filterMatchingTaggedCollection(activePolicies, t);
    }

    /**
     * Returns a list of all Active (not the ones waiting 4 handshake, as this data has not been imported yet) sharing
     * policies owned by a given user, which define the Sharing of the provided tagged collection.
     * 
     * @param user
     * @param t
     * @return
     */
    public List<SharingPolicy> getAllActiveSharingPoliciesOwnedByUserContainingTaggedCollection(User user,
            TaggedCollection t) {
        List<SharingPolicy> activePolicies = this.sharingPolicyDao.getAllSharingPoliciesFromUserInStateAndOfType(user,
                SharingPolicies.SHARE_TAGGED_COLLECTION, ActivityState.ACCEPTED_AND_ACTIVE);
        return filterMatchingTaggedCollection(activePolicies, t);
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

    /**
     * Takes a list of policies and checks for the ones that define the sharing of a given tagged collection
     * 
     * @param policies
     * @param t
     * @return
     */
    private List<SharingPolicy> filterMatchingTaggedCollection(List<SharingPolicy> policies, TaggedCollection t) {
        List<SharingPolicy> ret = new ArrayList<SharingPolicy>();
        //iterate over all policies and check if any matches the tagged collection we're looking for
        for (SharingPolicy policy : policies) {
            try {
                Long collID = Long.valueOf(policy.getSharedElementID());
                //check if we have a match
                if (collID.longValue() == t.getId().longValue()) {
                    ret.add(policy);
                }
            } catch (Exception e) {
                //ignore - probably a miss configured policy - should not happen
            }
        }
        return ret;
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
        return createAndAddSharingPolicy(owner, sharingWith, policy, null, name, description, null, null);
    }

    public SharingPolicy createAndAddSharingPolicy(User owner, User sharingWith, SharingPolicies policy,
            String sharedElementID, String name, String description, Date lifespanStartDate, Date lifespanEndDate) {
        //create the policy calling the default constructor
        SharingPolicy shPol = new SharingPolicy(owner, sharingWith, policy, sharedElementID, name, description);
        //check if we're setting a custom lifespan for the policy
        if (lifespanStartDate != null) {
            shPol.setPolicyLifeSpanStartDate(lifespanStartDate);
        }
        if (lifespanEndDate != null) {
            shPol.setPolicyLifeSpanEndDate(lifespanEndDate);
        }
        //add the sharing policy
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

    /**
     * Allows to update the metadata fields of a sharing policy e.g. name and description and to change the lifespan All
     * other fields cannot be updated for an already existing policy
     * 
     */
    public SharingPolicy updateSharingPolicy(User user, Long policyID, String name, String description,
            Date lifespanstart, Date lifespanend) {

        SharingPolicy p = this.sharingPolicyDao.getAllSharingPoliciesFromUserAndPolicyID(user, policyID);
        if (p != null) {
            if (name != null) {
                p.setName(name);
            }
            if (description != null) {
                p.setDescription(description);
            }
            if (lifespanstart != null) {
                p.setPolicyLifeSpanStartDate(lifespanstart);
            }
            if (lifespanend != null) {
                p.setPolicyLifeSpanEndDate(lifespanend);
            }
            p = this.sharingPolicyDao.merge(p);
            this.log.debug("updated sharing policy for user: " + user.id() + " and SharingPolicy " + p.getId());
            return p;
        } else {
            String s = "unable to update sharing for user: " + user.id() + " and SharingPolicy " + policyID
                    + " - policy does not exist";
            this.log.debug(s);
            throw new IllegalArgumentException(s);
        }
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
