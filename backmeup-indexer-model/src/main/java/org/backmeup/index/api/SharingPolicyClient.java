package org.backmeup.index.api;

import java.io.Closeable;
import java.util.Date;
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
            String description, Date lifespanstart, Date lifespanend);

    /**
     * It's only possible to update a already existing policy's name, description and lifespan. For all other fields you
     * need to delete and recreate
     */
    SharingPolicyEntry update(Long policyID, String name, String description, Date lifespanstart, Date lifespanend);

    String removeOwned(Long policyID);

    String removeAllOwned();

    String acceptIncomingSharing(Long policyID);

    String declineIncomingSharing(Long policyID);

    @Override
    void close();

}