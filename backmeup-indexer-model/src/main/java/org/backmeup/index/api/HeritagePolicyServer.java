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
public interface HeritagePolicyServer {

    Set<SharingPolicyEntry> getAllOwnedHeritagePolicies(User forUser);

    Set<SharingPolicyEntry> getAllIncomingHeritagePolicies(User forUser);

    SharingPolicyEntry addHeritagePolicy(User owner, User sharingWith, SharingPolicyTypeEntry policy,
            String sharedElementID, String name, String description, Date lifespanstart, Date lifespanend);

    SharingPolicyEntry updateHeritagePolicy(User owner, Long policyID, String name, String description,
            Date lifespanstart, Date lifespanend);

    String removeOwnedHeritagePolicy(User owner, Long policyID);

}