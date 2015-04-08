package org.backmeup.index.sharing.policy;

import java.util.List;

import javax.inject.Inject;

import org.backmeup.index.dal.SharingPolicyDao;
import org.backmeup.index.model.User;
import org.backmeup.index.utils.cdi.RunRequestScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Allows to define sharing policies e.g. User A shares IndexDocument with User B, User A shares Backup with User B,
 * etc.
 *
 */
//@ApplicationScoped
public class SharingPolicyManager {

    //TODO only allow sharing policy creation if the EntryStatus matches imported?
    //TODO REST Interface, get active user from system, do not accept others creating policies for this user
    //TODO Need to trigger Policy Change Event?

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
        return this.sharingPolicyDao.getAllSharingPoliciesFromUser(user);
    }

    public List<SharingPolicy> getAllActivePoliciesSharedWithUser(User user) {
        return this.sharingPolicyDao.getAllSharingPoliciesWithUser(user);
    }

    public List<SharingPolicy> getAllDeletedPoliciesForUser(User user) {
        //TODO implement need to keep a list of deleted policies so that we can perform content updates
        return null;
    }

    public SharingPolicy createAndAddSharingPolicy(User owner, User sharingWith, SharingPolicies policy) {
        SharingPolicy shPolicy = new SharingPolicy(owner, sharingWith, policy);
        return addSharingPolicy(shPolicy);
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
            String sharedElementID) {
        SharingPolicy shPol = createAndAddSharingPolicy(owner, sharingWith, policy);
        shPol.setSharedElementID(sharedElementID);
        return addSharingPolicy(shPol);
    }

    public SharingPolicy addSharingPolicy(SharingPolicy shPolicy) {
        List<SharingPolicy> existingPols = this.sharingPolicyDao.getAllSharingPoliciesBetweenUsersInType(new User(
                shPolicy.getFromUserID()), new User(shPolicy.getWithUserID()), shPolicy.getPolicy());
        if (existingPols.size() == 0) {
            return shPolicy = this.sharingPolicyDao.save(shPolicy);
        } else if (existingPols.size() > 0) {
            //in this case we need to check on the sharedElement ID and see if it already exists
            for (SharingPolicy pol : existingPols) {
                if ((shPolicy.getSharedElementID() != null) && (pol.getSharedElementID() != null)) {
                    //identical policies, both with the same sharedelement value
                    if (shPolicy.getSharedElementID().equals(pol.getSharedElementID())) {
                        return pol;
                    }
                } else if ((shPolicy.getSharedElementID() == null) && (pol.getSharedElementID() == null)) {
                    //identical policies, both without sharedelement value
                    return pol;
                }
            }
            //we didn't find it, so let's create it
            return shPolicy = this.sharingPolicyDao.save(shPolicy);
        }
        return null;
    }

    public void removeSharingPolicy(Long policyID) {
        SharingPolicy p = this.sharingPolicyDao.getByEntityId(policyID);
        removeSharingPolicy(p);
    }

    public void removeSharingPolicy(SharingPolicy p) {
        this.sharingPolicyDao.delete(p);
    }

    public void removeAllSharingPoliciesForUser(User owner) {
        for (SharingPolicy policy : this.sharingPolicyDao.getAllSharingPoliciesFromUser(owner)) {
            this.removeSharingPolicy(policy);
        }
    }
}
