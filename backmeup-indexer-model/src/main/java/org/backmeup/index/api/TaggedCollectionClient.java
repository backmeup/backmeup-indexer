package org.backmeup.index.api;

import java.io.Closeable;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.backmeup.index.model.tagging.TaggedCollectionEntry;

/**
 * A REST API client to the sharing policy component.
 * 
 */
public interface TaggedCollectionClient extends Closeable {

    Set<TaggedCollectionEntry> getAllTaggedCollections();

    Set<TaggedCollectionEntry> getAllTaggedCollectionsByNameQuery(String query);

    Set<TaggedCollectionEntry> getAllTaggedCollectionsContainingDocuments(List<UUID> lDocumentUUIDs);

    String removeTaggedCollection(Long collectionID);

    TaggedCollectionEntry createAndAddTaggedCollection(String name, String description, List<UUID> containedDocumentIDs);

    String addDocumentsToTaggedCollection(Long collectionID, List<UUID> documentIDs);

    String removeDocumentsFromTaggedCollection(Long collectionID, List<UUID> documentIDs);

    String removeAllCollectionsForUser();

    @Override
    void close();

}