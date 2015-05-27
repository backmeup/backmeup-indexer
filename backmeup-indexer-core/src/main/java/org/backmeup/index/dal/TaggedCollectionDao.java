package org.backmeup.index.dal;

import java.util.List;
import java.util.UUID;

import org.backmeup.index.model.User;
import org.backmeup.index.tagging.TaggedCollection;
import org.backmeup.index.tagging.TaggedCollection.ActivityState;

/**
 * The TaggedCollectionDao contains all database relevant operations for the model class TaggedCollection
 */
public interface TaggedCollectionDao extends BaseDao<TaggedCollection> {

    TaggedCollection getByEntityId(Long entityId);

    TaggedCollection getByEntityIdAndState(Long entityId, ActivityState... state);

    List<TaggedCollection> getAllActiveFromUser(User user);

    List<TaggedCollection> getAllFromUserAndInState(User user, ActivityState... state);

    List<TaggedCollection> getAllActiveFromUserAndLikeName(User user, String name);

    List<TaggedCollection> getAllFromUserAndLikeNameAndInState(User user, String name, ActivityState... state);

    List<TaggedCollection> getAllActiveFromUserContainingDocumentIds(User user, List<UUID> documentIds);

    List<TaggedCollection> getAllFromUserContainingDocumentIdsAndInState(User user, List<UUID> documentIds,
            ActivityState... state);

    @Deprecated
    void deleteAll();

}