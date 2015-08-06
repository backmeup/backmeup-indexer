package org.backmeup.index.api;

import java.util.Date;
import java.util.Set;

import org.backmeup.index.model.User;
import org.backmeup.index.model.sharing.SharingPolicyEntry;
import org.backmeup.index.model.sharing.SharingPolicyEntry.SharingPolicyTypeEntry;

/**
 * Artificial interface to keep the client and the server of REST API in sync.
 * 
 */
public interface SharingPolicyServer extends HeritagePolicyServer{

    Set<SharingPolicyEntry> getAllOwnedSharingPolicies(User forUser);

    Set<SharingPolicyEntry> getAllIncomingSharingPolicies(User forUser);

    SharingPolicyEntry addSharingPolicy(User owner, User sharingWith, SharingPolicyTypeEntry policy, String sharedElementID,
            String name, String description, Date lifespanstart, Date lifespanend);

    /**
     * It's only possible to update a already existing policy's name, description and lifespan. For all other fields you
     * need to delete and recreate
     * 
     */
    SharingPolicyEntry updateSharingPolicy(User owner, Long policyID, String name, String description, Date lifespanstart,
            Date lifespanend);

    String removeOwnedSharingPolicy(User owner, Long policyID);

    String removeAllOwnedSharingPolicies(User owner);

    String acceptIncomingSharing(User user, Long policyID);

    String declineIncomingSharing(User user, Long policyID);

}