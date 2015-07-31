package org.backmeup.index.dal;

import java.util.List;

import org.backmeup.index.model.User;
import org.backmeup.index.sharing.policy.SharingPolicies;
import org.backmeup.index.sharing.policy.SharingPolicy;
import org.backmeup.index.sharing.policy.SharingPolicy.ActivityState;

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

    SharingPolicy getAllSharingPoliciesFromUserAndPolicyID(User withUser, Long policyID);

    List<SharingPolicy> getAllSharingPoliciesFromUserInState(User fromUser, ActivityState... state);

    List<SharingPolicy> getAllSharingPoliciesFromUserInStateAndOfType(User fromUser, SharingPolicies type,
            ActivityState... state);

    List<SharingPolicy> getAllSharingPoliciesWithUser(User withUser);

    SharingPolicy getAllSharingPoliciesWithUserAndPolicyID(User withUser, Long policyID);

    List<SharingPolicy> getAllSharingPoliciesWithUserInState(User withUser, ActivityState... state);

    List<SharingPolicy> getAllSharingPoliciesBetweenUsers(User fromUser, User withUser);

    List<SharingPolicy> getAllSharingPoliciesBetweenUserInState(User fromUser, User withUser, ActivityState... state);

    List<SharingPolicy> getAllSharingPoliciesBetweenUsersInType(User fromUser, User withUser, SharingPolicies... types);

    void deleteAll();

}