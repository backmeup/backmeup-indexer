package org.backmeup.index.dal;

import java.util.List;

import org.backmeup.index.model.User;
import org.backmeup.index.sharing.policy.SharingPolicy;

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

    List<SharingPolicy> getAllHeritagePoliciesWithUser(User withUser);

    SharingPolicy getHeritagePolicyWithUserAndPolicyID(User withUser, Long policyID);

    SharingPolicy getHeritagePolicyFromUserAndPolicyID(User fromUser, Long policyID);

    List<SharingPolicy> getAllHeritagePoliciesBetweenUsers(User fromUser, User withUser);

    /**
     * Pulls the dea man switch and activates the heritage passed from fromUser (heritage owner) to withUser (heritage
     * receiver)
     * 
     * @param deadUser
     *            from user
     * @param withUser
     * @return
     */
    void activateHeritage(User deadUser, User withUser);

}