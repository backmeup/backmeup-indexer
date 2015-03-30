package org.backmeup.index.dal;

import java.util.List;

import org.backmeup.index.model.User;
import org.backmeup.index.sharing.policy.SharingPolicy;

/**
 * The SharingPolicyDao contains all database relevant operations for the model class SharingPolicy
 */
public interface SharingPolicyDao extends BaseDao<SharingPolicy> {

    /**
     * Find an element by its DB entity id.
     * 
     * @param entityId
     * @return
     */
    SharingPolicy getByEntityId(Long entityId);

    List<SharingPolicy> getAllSharingPolicies();

    List<SharingPolicy> getAllSharingPoliciesFromUser(User fromUser);

    List<SharingPolicy> getAllSharingPoliciesWithUser(User withUser);

    List<SharingPolicy> getAllSharingPoliciesBetweenUsers(User fromUser, User withUser);

    void deleteAll();

}