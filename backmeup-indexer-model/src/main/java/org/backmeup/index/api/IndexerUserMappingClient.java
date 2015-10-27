package org.backmeup.index.api;

import java.io.Closeable;
import java.io.IOException;

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

    /**
     * Lookup the corresponding Backmeup User ID for a given Keyserver User ID
     * 
     * @param keyserverUserId
     * @return
     */
    Long getBMUUserID(String keyserverUserId) throws IOException;

    /**
     * Lookup the corresponding Keyserver User ID for a given Backmeup User ID
     * 
     * @param BMUUserId
     * @return
     */
    String getKeyserverUserID(Long BMUUserId) throws IOException;

    @Override
    void close();

}