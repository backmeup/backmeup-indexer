package org.backmeup.index.dal.jpa;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.enterprise.context.RequestScoped;
import javax.persistence.TypedQuery;

import org.backmeup.index.dal.TaggedCollectionDao;
import org.backmeup.index.model.User;
import org.backmeup.index.tagging.TaggedCollection;

@RequestScoped
public class TaggedCollectionDaoImpl extends BaseDaoImpl<TaggedCollection> implements TaggedCollectionDao {

    private static final String TABLENAME = TaggedCollection.class.getSimpleName();

    public TaggedCollectionDaoImpl() {
        super(TaggedCollection.class);
    }

    @Override
    public TaggedCollection getByEntityId(Long entityId) {
        TypedQuery<TaggedCollection> q = createTypedQuery("SELECT u FROM " + TABLENAME + " u WHERE u.id = :entityId");
        q.setParameter("entityId", entityId);
        return executeQuerySelectFirst(q);
    }

    @Override
    public List<TaggedCollection> getAllFromUser(User user) {
        TypedQuery<TaggedCollection> q = createTypedQuery("SELECT u FROM " + TABLENAME
                + " u WHERE u.userId = :user ORDER BY u.id ASC");
        q.setParameter("user", user.id());
        return executeQuery(q);
    }

    @Override
    public List<TaggedCollection> getAllFromUserAndName(User user, String name) {
        TypedQuery<TaggedCollection> q = createTypedQuery("SELECT u FROM " + TABLENAME
                + " u WHERE u.userId = :user and u.name = :name ORDER BY u.id ASC");
        q.setParameter("user", user.id());
        q.setParameter("name", name);
        return executeQuery(q);
    }

    @Override
    public List<TaggedCollection> getAllFromUserContainingDocumentIds(User user, List<UUID> requiredDDocUUIDs) {
        List<TaggedCollection> ret = new ArrayList<TaggedCollection>();
        TypedQuery<TaggedCollection> q = createTypedQuery("SELECT u FROM " + TABLENAME
                + " u WHERE u.userId = :user ORDER BY u.id ASC");
        q.setParameter("user", user.id());
        List<TaggedCollection> collections = executeQuery(q);
        //Note: as we're using  @ElementCollection() for the Hibernate Entity we can't use a proper JOIN
        //to query to oneTwoMany Table but need to work on the parent object. So for simplicity we
        //evaluate the matching criteria within this for statement
        //@see http://stackoverflow.com/questions/3708914/jpa-2-using-elementcollection-in-criteriaquery
        for (TaggedCollection collection : collections) {
            List<UUID> allCollUUIDs = collection.getDocumentIds();
            boolean ok = allCollUUIDs.containsAll(requiredDDocUUIDs);
            if (ok) {
                ret.add(collection);
            }
        }
        return ret;
    }

    private TypedQuery<TaggedCollection> createTypedQuery(String sql) {
        return this.entityManager.createQuery(sql, TaggedCollection.class);
    }

    private List<TaggedCollection> executeQuery(TypedQuery<TaggedCollection> q) {
        List<TaggedCollection> status = q.getResultList();
        if (status != null && status.size() > 0) {
            return status;
        }
        return new ArrayList<>();
    }

    private TaggedCollection executeQuerySelectFirst(TypedQuery<TaggedCollection> q) {
        List<TaggedCollection> status = executeQuery(q);
        return status.size() > 0 ? status.get(0) : null;
    }

    @Transactional
    @Override
    public void deleteAll() {
        this.entityManager.createQuery("DELETE FROM +" + TABLENAME).executeUpdate();
    }

}
