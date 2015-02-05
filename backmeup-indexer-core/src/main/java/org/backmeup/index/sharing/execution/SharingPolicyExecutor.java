package org.backmeup.index.sharing.execution;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.backmeup.index.model.IndexDocument;

/**
 * Checks on the SharingPolicies and takes care of distributing the IndexDocuments into the secure drop off zones for
 * the individual users.
 *
 */
public class SharingPolicyExecutor {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private void saveForOwner(IndexDocument doc) {

    }

    private void saveForSharingPartners(IndexDocument doc) {

    }

}
