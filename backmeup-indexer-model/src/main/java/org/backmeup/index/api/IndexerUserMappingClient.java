package org.backmeup.index.api;

import java.io.Closeable;

/**
 * A REST API client to the user mapping helper component.
 * 
 */
public interface IndexerUserMappingClient extends Closeable {

    /**
     * Creates/updates a database table mapping for a user: bmuUserId with corresponding keyserverUserId
     * 
     * @param bmuUserId
     * @param keyserverUserId
     * @return
     */
    String updateUserMapping(Long bmuUserId, String keyserverUserId) throws IllegalArgumentException, IllegalStateException;

    @Override
    void close();

}