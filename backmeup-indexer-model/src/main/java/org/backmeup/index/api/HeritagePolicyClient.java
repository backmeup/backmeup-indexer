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
public interface HeritagePolicyClient extends Closeable {

    Set<SharingPolicyEntry> getAllOwnedHeritagePolicies();

    Set<SharingPolicyEntry> getAllIncomingHeritagePolicies();

    SharingPolicyEntry addHeritagePolicy(User sharingWith, SharingPolicyTypeEntry policy, String sharedElementID,
            String name, String description, Date lifespanstart, Date lifespanend);

    SharingPolicyEntry updateHeritagePolicy(Long policyID, String name, String description, Date lifespanstart,
            Date lifespanend);

    String removeOwnedHeritagePolicy(Long policyID);

    /**
     * Activates the dead man switch for the current user. So all incoming heritage policies shared with this user will
     * be activated.
     */
    String activateDeadMannSwitchAndImport();

    @Override
    void close();

}