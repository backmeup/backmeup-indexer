package org.backmeup.index.sharing.policy;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.backmeup.index.dal.HeritagePolicyDao;
import org.backmeup.index.dal.SharingPolicyDao;
import org.backmeup.index.model.User;
import org.backmeup.index.sharing.policy.SharingPolicy.ActivityState;
import org.backmeup.index.sharing.policy.SharingPolicy.Type;
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
    @Inject
    private HeritagePolicyDao heritagePolicyDao;

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

    public List<SharingPolicy> getAllWaiting4HandshakeAndScheduledAndActivePoliciesOwnedByUser(User user) {
        return this.sharingPolicyDao.getAllSharingPoliciesFromUserInState(user, ActivityState.ACCEPTED_AND_ACTIVE,
                ActivityState.ACCEPTED_AND_WAITING_FOR_TIMSPAN_START, ActivityState.CREATED_AND_WAITING_FOR_HANDSHAKE);
    }

    /**
     * Returns a list of all Active and Waiting4Handshake sharing policies owned by a given user which define the
     * Sharing of the provided tagged collection.
     * 
     * @param user
     * @param t
     * @return
     */
    public List<SharingPolicy> getAllWaiting4HandshakeAndScheduledAndActivePoliciesOwnedByUserContainingTaggedCollection(
            User user, TaggedCollection t) {
        List<SharingPolicy> activePolicies = this.sharingPolicyDao.getAllSharingPoliciesFromUserInState(user,
                ActivityState.ACCEPTED_AND_ACTIVE, ActivityState.ACCEPTED_AND_WAITING_FOR_TIMSPAN_START,
                ActivityState.CREATED_AND_WAITING_FOR_HANDSHAKE);
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

    public List<SharingPolicy> getAllWaiting4HandshakeAndScheduledAndActivePoliciesSharedWithUser(User user) {
        return this.sharingPolicyDao.getAllSharingPoliciesWithUserInState(user, ActivityState.ACCEPTED_AND_ACTIVE,
                ActivityState.ACCEPTED_AND_WAITING_FOR_TIMSPAN_START, ActivityState.CREATED_AND_WAITING_FOR_HANDSHAKE);
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
        return createAndAddSharingPolicy(owner, sharingWith, policy, sharedElementID, name, description, null, null);
    }

    public SharingPolicy createAndAddSharingPolicy(User owner, User sharingWith, SharingPolicies policy,
            String sharedElementID, String name, String description, Date lifespanStartDate, Date lifespanEndDate) {
        return createAndAddPolicy(owner, sharingWith, policy, sharedElementID, name, description, lifespanStartDate,
                lifespanEndDate, Type.SHARING);
    }

    private SharingPolicy createAndAddPolicy(User owner, User sharingWith, SharingPolicies policy,
            String sharedElementID, String name, String description, Date lifespanStartDate, Date lifespanEndDate,
            Type t) {
        //create the policy calling the default constructor
        SharingPolicy shPol = new SharingPolicy(owner, sharingWith, policy, sharedElementID, name, description);
        //distinguish between standard sharing and heritage policy
        if (t.equals(Type.HERITAGE)) {
            shPol.initHeritagePolicy();
        }
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
        return updatePolicy(user, policyID, p, name, description, lifespanstart, lifespanend);
    }

    /**
     * private helper to update sharing and heritage policies
     */
    private SharingPolicy updatePolicy(User user, Long policyID, SharingPolicy p, String name, String description,
            Date lifespanstart, Date lifespanend) {
        if (p != null) {
            if (name != null) {
                p.setName(name);
            }
            if (description != null) {
                p.setDescription(description);
            }
            //only allow editing the lifespan of Sharing not of Heritage policies
            if ((p.getType().equals(Type.SHARING)) && (lifespanstart != null)) {
                //we don't allow to modify already active policies
                if (!p.getState().equals(ActivityState.ACCEPTED_AND_ACTIVE)) {
                    p.setPolicyLifeSpanStartDate(lifespanstart);
                }
            }
            //only allow editing the lifespan of Sharing not of Heritage policies
            if ((p.getType().equals(Type.SHARING)) && (lifespanend != null)) {
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
        removePolicy(p);
    }

    private void removePolicy(SharingPolicy p) {
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
            p.setState(ActivityState.ACCEPTED_AND_WAITING_FOR_TIMSPAN_START);
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

    public List<SharingPolicy> getAllHeritagePoliciesOwnedByUser(User user) {
        return this.heritagePolicyDao.getAllHeritagePoliciesFromUser(user);
    }

    public List<SharingPolicy> getAllHeritagePoliciesSharedWithUser(User user) {
        return this.heritagePolicyDao.getAllHeritagePoliciesWithUser(user);
    }

    public SharingPolicy createAndAddHeritagePolicy(User owner, User sharingWith, SharingPolicies policy,
            String sharedElementID, String name, String description, Date lifespanStartDate, Date lifespanEndDate) {
        return createAndAddPolicy(owner, sharingWith, policy, sharedElementID, name, description, lifespanStartDate,
                lifespanEndDate, Type.HERITAGE);
    }

    public SharingPolicy updateHeritagePolicy(User user, Long policyID, String name, String description,
            Date lifespanstart, Date lifespanend) {
        SharingPolicy p = this.heritagePolicyDao.getHeritagePolicyFromUserAndPolicyID(user, policyID);
        return updatePolicy(user, policyID, p, name, description, lifespanstart, lifespanend);
    }

    public void removeHeritagePolicy(Long policyID) {
        SharingPolicy p = this.heritagePolicyDao.getByEntityId(policyID);
        removePolicy(p);
        this.log.debug("updated HeritagePolicy for deletion: " + p.toString());
    }

    public void activateDeadManSwitchAndImport(User currUser) {
        //as heritage is only shared 1:1 between acounts we enable import of all heritage sharing policies 
        this.heritagePolicyDao.acceptAndActivateHeritage(currUser);
    }
}
