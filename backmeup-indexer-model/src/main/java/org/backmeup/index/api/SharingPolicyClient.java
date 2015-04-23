package org.backmeup.index.api;

import java.io.Closeable;
import java.util.Set;

import org.backmeup.index.model.User;
import org.backmeup.index.model.sharing.SharingPolicyEntry;
import org.backmeup.index.model.sharing.SharingPolicyEntry.SharingPolicyTypeEntry;

/**
 * A REST API client to the sharing policy component.
 * 
 */
public interface SharingPolicyClient extends Closeable {

    Set<SharingPolicyEntry> getAllOwned();

    Set<SharingPolicyEntry> getAllIncoming();

    SharingPolicyEntry add(User sharingWith, SharingPolicyTypeEntry policy, String sharedElementID, String name,
            String description);

    String removeOwned(Long policyID);

    String removeAllOwned();

    @Override
    void close();

}