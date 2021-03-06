package org.backmeup.index.dal;

import java.util.List;

import org.backmeup.index.model.User;
import org.backmeup.index.sharing.policy.SharingPolicy;
import org.backmeup.index.sharing.policy.SharingPolicy.ActivityState;

/**
 * The HeritagePolicyDao contains all database relevant operations for the model class SharingPolicy which are specific
 * for the use case 'vererben'
 */
public interface HeritagePolicyDao extends BaseDao<SharingPolicy> {

    /**
     * Find an element by its DB entity id.
     * 
     * @param entityId
     * @return
     */
    SharingPolicy getByEntityId(Long entityId);

    List<SharingPolicy> getAllHeritagePolicies();

    List<SharingPolicy> getAllHeritagePoliciesFromUser(User fromUser);

    List<SharingPolicy> getAllSharingPoliciesFromUserInState(User fromUser, ActivityState... state);

    List<SharingPolicy> getAllHeritagePoliciesWithUser(User withUser);

    List<SharingPolicy> getAllSharingPoliciesWithUserInState(User fromUser, ActivityState... state);

    SharingPolicy getHeritagePolicyWithUserAndPolicyID(User withUser, Long policyID);

    SharingPolicy getHeritagePolicyFromUserAndPolicyID(User fromUser, Long policyID);

    List<SharingPolicy> getAllHeritagePoliciesBetweenUsers(User fromUser, User withUser);

    /**
     * Pulls the dea man switch and activates the heritage passed on to withUser (heritage receiver)
     *
     * @param withUser
     * @return
     */
    void acceptAndActivateHeritage(User withUser);

}