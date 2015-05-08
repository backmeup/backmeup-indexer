package org.backmeup.index.dal;

import java.util.List;
import java.util.UUID;

import org.backmeup.index.model.User;
import org.backmeup.index.tagging.TaggedCollection;

/**
 * The TaggedCollectionDao contains all database relevant operations for the model class TaggedCollection
 */
public interface TaggedCollectionDao extends BaseDao<TaggedCollection> {

    TaggedCollection getByEntityId(Long entityId);

    List<TaggedCollection> getAllFromUser(User user);

    List<TaggedCollection> getAllFromUserAndLikeName(User user, String name);

    List<TaggedCollection> getAllFromUserContainingDocumentIds(User user, List<UUID> documentIds);

    void deleteAll();

}