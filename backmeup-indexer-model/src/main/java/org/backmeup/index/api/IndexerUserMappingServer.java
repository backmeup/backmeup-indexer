package org.backmeup.index.api;

/**
 * Artificial interface to keep the client and the server of REST API in sync.
 * 
 */
public interface IndexerUserMappingServer {

    /**
     * Creates/updates a database table mapping for a user: bmuUserId with corresponding keyserverUserId
     * 
     * @param bmuUserId
     * @param keyserverUserId
     * @return
     */
    String updateUserMapping(Long bmuUserId, String keyserverUserId);

}