package org.backmeup.index.tagging;

import java.util.List;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.backmeup.index.dal.TaggedCollectionDao;
import org.backmeup.index.model.User;
import org.backmeup.index.utils.cdi.RunRequestScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Allows to define tagged collections i.e. select documents of different backups and define them as a custom collection
 *
 */
@ApplicationScoped
public class TaggedCollectionManager {

    //TODO create addSharingPolicyForTaggedCollection?

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    private TaggedCollectionDao taggedCollectionDao;

    public TaggedCollectionManager() {
    }

    @RunRequestScoped
    public void startupTaggedCollectionManager() {
        this.log.debug("startup() TaggedCollectionManager (ApplicationScoped) completed");
    }

    @RunRequestScoped
    public void shutdownTaggedCollectionManager() {
        this.log.debug("shutdown() TaggedCollectionManager (ApplicationScoped) completed");
    }

    public List<TaggedCollection> getAllTaggedCollections(User user) {
        return this.taggedCollectionDao.getAllFromUser(user);
    }

    public List<TaggedCollection> getAllTaggedCollectionsByNameQuery(User user, String query) {
        return this.taggedCollectionDao.getAllFromUserAndLikeName(user, query);
    }

    public List<TaggedCollection> getAllTaggedCollectionsContainingDocuments(User user, List<UUID> lDocumentUUIDs) {
        return this.taggedCollectionDao.getAllFromUserContainingDocumentIds(user, lDocumentUUIDs);
    }

    public void removeTaggedCollection(Long collectionID) {
        TaggedCollection t = this.taggedCollectionDao.getByEntityId(collectionID);
        if (t == null) {
            this.log.debug("failed to remove TaggedCollection: " + collectionID + " due to: collection not found");
        } else {
            removeTaggedCollection(t);
        }
    }

    public void removeTaggedCollection(TaggedCollection t) {
        this.taggedCollectionDao.delete(t);
        this.log.debug("deleted TaggedCollection: " + t.getId());
        //TODO AL: WHAT HAPPENS IF WE SEARCH FOR DOCUMENTS THAT ARENT THERE ANYMORE? (e.g. sharing revoked ones)
        //Possible we need something like this: just set the state, deletion from dao will be handled by the SharingPolicyUp2DateCheckerTask
        //t.setState(ActivityState.WAITING_FOR_DELETION);
        //this.sharingPolicyDao.merge(p);
    }

    public TaggedCollection createAndAddTaggedCollection(User user, String name, String description) {
        return createAndAddTaggedCollection(user, name, description, null);
    }

    public TaggedCollection createAndAddTaggedCollection(User user, String name, String description,
            List<UUID> containedDocumentIDs) {
        TaggedCollection taggedColl = new TaggedCollection(user, name, description, containedDocumentIDs);
        return addTaggedCollection(taggedColl);
    }

    public int addDocumentsToTaggedCollection(Long collectionID, List<UUID> documentIDs) {
        TaggedCollection t = this.taggedCollectionDao.getByEntityId(collectionID);
        if (t == null) {
            String s = "failed adding documents for TaggedCollection: " + collectionID
                    + " due to: collection not found";
            this.log.debug(s);
            throw new IllegalArgumentException(s);
        }
        int count = 0;
        List<UUID> docs = t.getDocumentIds();
        for (UUID documentID : documentIDs) {
            if (!docs.contains(documentID)) {
                t.addDocumentId(documentID);
                count++;
            }
        }
        this.taggedCollectionDao.merge(t);
        this.log.debug("added " + count + " documents to taggedCollection: " + collectionID);
        return count;
    }

    public int removeDocumentsFromTaggedCollection(Long collectionID, List<UUID> documentIDs) {
        TaggedCollection t = this.taggedCollectionDao.getByEntityId(collectionID);
        if (t == null) {
            String s = "failed remove documents for TaggedCollection: " + collectionID
                    + " due to: collection not found";
            this.log.debug(s);
            throw new IllegalArgumentException(s);
        }
        int count = 0;
        List<UUID> docs = t.getDocumentIds();
        for (UUID documentID : documentIDs) {
            if (docs.contains(documentID)) {
                t.removeDocumentId(documentID);
                count++;
            }
        }
        this.taggedCollectionDao.merge(t);
        this.log.debug("removed " + count + " documents from taggedCollection: " + collectionID);
        return count;
    }

    public TaggedCollection addTaggedCollection(TaggedCollection taggedColl) {
        taggedColl = this.taggedCollectionDao.save(taggedColl);
        this.log.debug("added TaggedCollection " + taggedColl.toString());
        return taggedColl;
    }

    public void removeAllTaggedCollectionsForUser(User user) {
        for (TaggedCollection taggedColl : this.taggedCollectionDao.getAllFromUser(user)) {
            this.removeTaggedCollection(taggedColl);
        }
    }
}
