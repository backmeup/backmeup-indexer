package org.backmeup.index.api;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.backmeup.index.model.User;
import org.backmeup.index.model.tagging.TaggedCollectionEntry;

/**
 * Artificial interface to keep the client and the server of REST API in sync.
 * 
 */
public interface TaggedCollectionServer {

    Set<TaggedCollectionEntry> getAllTaggedCollections(User user);

    Set<TaggedCollectionEntry> getAllTaggedCollectionsByNameQuery(User user, String query);

    Set<TaggedCollectionEntry> getAllTaggedCollectionsContainingDocuments(User user, List<UUID> lDocumentUUIDs);

    String removeTaggedCollection(User user, Long collectionID);

    TaggedCollectionEntry createAndAddTaggedCollection(User user, String name, String description,
            List<UUID> containedDocumentIDs);

    String addDocumentsToTaggedCollection(User user, Long collectionID, List<UUID> documentIDs);

    String removeDocumentsFromTaggedCollection(User user, Long collectionID, List<UUID> documentIDs);

    String removeAllCollectionsForUser(User user);

}