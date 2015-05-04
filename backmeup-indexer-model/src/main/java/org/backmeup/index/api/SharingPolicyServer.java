package org.backmeup.index.api;

import java.util.Set;

import org.backmeup.index.model.User;
import org.backmeup.index.model.sharing.SharingPolicyEntry;
import org.backmeup.index.model.sharing.SharingPolicyEntry.SharingPolicyTypeEntry;

/**
 * Artificial interface to keep the client and the server of REST API in sync.
 * 
 */
public interface SharingPolicyServer {

    Set<SharingPolicyEntry> getAllOwned(User forUser);

    Set<SharingPolicyEntry> getAllIncoming(User forUser);

    SharingPolicyEntry add(User owner, User sharingWith, SharingPolicyTypeEntry policy, String sharedElementID,
            String name, String description);

    String removeOwned(User owner, Long policyID);

    String removeAllOwned(User owner);

    String acceptIncomingSharing(User user, Long policyID);

    String declineIncomingSharing(User user, Long policyID);

}