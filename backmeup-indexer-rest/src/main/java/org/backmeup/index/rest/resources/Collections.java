package org.backmeup.index.rest.resources;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.backmeup.index.api.TaggedCollectionServer;
import org.backmeup.index.core.model.IndexFragmentEntryStatus.StatusType;
import org.backmeup.index.dal.IndexFragmentEntryStatusDao;
import org.backmeup.index.model.User;
import org.backmeup.index.model.tagging.TaggedCollectionEntry;
import org.backmeup.index.tagging.TaggedCollection;
import org.backmeup.index.tagging.TaggedCollectionManager;

@Path("collections")
@Produces(MediaType.APPLICATION_JSON)
public class Collections extends ParameterValidator implements TaggedCollectionServer {

    @Inject
    private TaggedCollectionManager collectionManager;
    @Inject
    private IndexFragmentEntryStatusDao indexEntryDao;

    @GET
    @Path("/{userId}")
    public Set<TaggedCollectionEntry> getTaggedCollectionsRS(//
            @PathParam("userId") User user,//
            @QueryParam("containsName") String name,//
            @QueryParam("containsDocs") List<UUID> lDocumentUUIDs) {

        mandatory("userId", user);

        if ((lDocumentUUIDs != null) && (lDocumentUUIDs.size() > 0)) {
            return this.getAllTaggedCollectionsContainingDocuments(user, lDocumentUUIDs);
        } else if ((name != null) && (!name.equals(""))) {
            return this.getAllTaggedCollectionsByNameQuery(user, name);
        } else {
            return this.getAllTaggedCollections(user);
        }
    }

    @Override
    public Set<TaggedCollectionEntry> getAllTaggedCollections(User user) {
        List<TaggedCollection> tc = this.collectionManager.getAllTaggedCollections(user);
        return convert(tc);
    }

    @Override
    public Set<TaggedCollectionEntry> getAllTaggedCollectionsByNameQuery(User user, String query) {
        List<TaggedCollection> tc = this.collectionManager.getAllTaggedCollectionsByNameQuery(user, query);
        return convert(tc);
    }

    @Override
    public Set<TaggedCollectionEntry> getAllTaggedCollectionsContainingDocuments(User user, List<UUID> lDocumentUUIDs) {
        List<TaggedCollection> tc = this.collectionManager.getAllTaggedCollectionsContainingDocuments(user,
                lDocumentUUIDs);
        return convert(tc);
    }

    @DELETE
    @Path("/{userId}")
    public String removeTaggedCollectionRS(//
            @PathParam("userId") User user,//
            @QueryParam("collectionId") Long collectionID) {

        mandatory("userId", user);

        if (collectionID != null && collectionID != -1) {
            return this.removeTaggedCollection(collectionID);
        } else {
            return this.removeAllCollectionsForUser(user);
        }
    }

    @Override
    public String removeAllCollectionsForUser(User user) {
        try {
            this.collectionManager.removeAllTaggedCollectionsForUser(user);
            return "deleted all tagged collections for user: " + user.id();
        } catch (Exception e) {
            return "unable to remove all tagged collections for user: " + user.id() + " due to: " + e.toString();
        }
    }

    @Override
    public String removeTaggedCollection(Long collectionID) {
        try {
            this.collectionManager.removeTaggedCollection(collectionID);
            return "removed tagged collections: " + collectionID;
        } catch (Exception e) {
            return "unable to remove tagged collection: " + collectionID + " due to: " + e.toString();
        }
    }

    @Override
    @POST
    @Path("/{userId}")
    public TaggedCollectionEntry createAndAddTaggedCollection(//
            @PathParam("userId") User user,//
            @QueryParam("name") String name,//
            @QueryParam("description") String description, @QueryParam("documentIds") List<UUID> containedDocumentIDs) {

        mandatory("userId", user);

        TaggedCollection tc = this.collectionManager.createAndAddTaggedCollection(user, name, description,
                containedDocumentIDs);
        return convert(tc);
    }

    @Override
    @POST
    @Path("/adddocuments/{collId}")
    public String addDocumentsToTaggedCollection(//
            @PathParam("collId") Long collectionID,//
            @QueryParam("documentIds") List<UUID> documentIDs) {
        mandatory("collId", collectionID);
        mandatory("documentIds", documentIDs);

        try {
            this.collectionManager.addDocumentsToTaggedCollection(collectionID, documentIDs);
            return "successfully added " + documentIDs.size() + " documents to collection: " + collectionID;
        } catch (Exception e) {
            return "unable to add documents to collection due to: " + e.toString();
        }
    }

    @Override
    @DELETE
    @Path("/removedocuments/{collId}")
    public String removeDocumentsFromTaggedCollection(//
            @PathParam("collId") Long collectionID,//
            @QueryParam("documentIds") List<UUID> documentIDs) {
        mandatory("collId", collectionID);
        mandatory("documentIds", documentIDs);

        try {
            this.collectionManager.removeDocumentsFromTaggedCollection(collectionID, documentIDs);
            return "successfully removed " + documentIDs.size() + " documents from collection: " + collectionID;
        } catch (Exception e) {
            return "unable to remove documents from collection due to: " + e.toString();
        }
    }

    private TaggedCollectionEntry convert(TaggedCollection t) {
        int docCount = this.indexEntryDao.getAllFromUserInOneOfTheTypes(new User(t.getUserId()), StatusType.IMPORTED,
                StatusType.WAITING_FOR_IMPORT).size();
        TaggedCollectionEntry e = new TaggedCollectionEntry(t.getId(), new User(t.getUserId()), t.getName(),
                t.getDescription(), t.getCollectionCreationDate(), t.getDocumentIds(), docCount);
        return e;
    }

    private Set<TaggedCollectionEntry> convert(List<TaggedCollection> tc) {
        Set<TaggedCollectionEntry> ret = new HashSet<TaggedCollectionEntry>();

        for (TaggedCollection t : tc) {
            TaggedCollectionEntry e = convert(t);
            ret.add(e);
        }
        return ret;
    }

}
