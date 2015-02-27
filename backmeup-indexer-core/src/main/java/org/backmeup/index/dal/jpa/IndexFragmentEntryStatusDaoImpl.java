package org.backmeup.index.dal.jpa;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.enterprise.context.RequestScoped;
import javax.persistence.TypedQuery;

import org.backmeup.index.core.model.IndexFragmentEntryStatus;
import org.backmeup.index.core.model.IndexFragmentEntryStatus.StatusType;
import org.backmeup.index.dal.IndexFragmentEntryStatusDao;
import org.backmeup.index.model.User;

@RequestScoped
public class IndexFragmentEntryStatusDaoImpl extends BaseDaoImpl<IndexFragmentEntryStatus> implements
        IndexFragmentEntryStatusDao {

    private static final String TABLENAME = IndexFragmentEntryStatus.class.getSimpleName();

    public IndexFragmentEntryStatusDaoImpl() {
        super(IndexFragmentEntryStatus.class);
    }

    @Transactional
    @Override
    public void deleteAll() {
        this.entityManager.createQuery("DELETE FROM +" + TABLENAME).executeUpdate();
    }

    private TypedQuery<IndexFragmentEntryStatus> createTypedQuery(String sql) {
        return this.entityManager.createQuery(sql, IndexFragmentEntryStatus.class);
    }

    private List<IndexFragmentEntryStatus> executeQuery(TypedQuery<IndexFragmentEntryStatus> q) {
        List<IndexFragmentEntryStatus> status = q.getResultList();
        if (status != null && status.size() > 0) {
            return status;
        }
        return new ArrayList<>();
    }

    private IndexFragmentEntryStatus executeQuerySelectFirst(TypedQuery<IndexFragmentEntryStatus> q) {
        List<IndexFragmentEntryStatus> status = executeQuery(q);
        return status.size() > 0 ? status.get(0) : null;
    }

    @Override
    public List<IndexFragmentEntryStatus> getAllIndexFragmentEntryStatus(User user) {
        TypedQuery<IndexFragmentEntryStatus> q = createTypedQuery("SELECT u FROM " + TABLENAME
                + " u WHERE u.userID = :userId ORDER BY u.id ASC");
        q.setParameter("userId", user.id());
        return executeQuery(q);
    }

    @Override
    public List<IndexFragmentEntryStatus> getAllIndexFragmentEntryStatus(User user, StatusType type) {
        TypedQuery<IndexFragmentEntryStatus> q = createTypedQuery("SELECT u FROM " + TABLENAME
                + " u WHERE u.userID = :userId and u.statusType = :statusType ORDER BY u.id ASC");
        q.setParameter("userId", user.id());
        q.setParameter("statusType", type);
        return executeQuery(q);
    }

    @Override
    public List<IndexFragmentEntryStatus> getAllIndexFragmentEntryStatus(UUID documentUUID) {
        TypedQuery<IndexFragmentEntryStatus> q = createTypedQuery("SELECT u FROM " + TABLENAME
                + " u WHERE u.documentUUID = :documentUUID ORDER BY u.id ASC");
        q.setParameter("documentUUID", documentUUID);
        return executeQuery(q);
    }

    @Override
    public List<IndexFragmentEntryStatus> getAllIndexFragmentEntryStatus(StatusType type) {
        TypedQuery<IndexFragmentEntryStatus> q = createTypedQuery("SELECT u FROM " + TABLENAME
                + " u WHERE u.statusType = :statusType ORDER BY u.id ASC");
        q.setParameter("statusType", type);
        return executeQuery(q);
    }

    @Override
    public IndexFragmentEntryStatus getIndexFragmentEntryStatustByEntityId(Long entityId) {
        TypedQuery<IndexFragmentEntryStatus> q = createTypedQuery("SELECT u FROM " + TABLENAME
                + " u WHERE u.Id = :entityId");
        q.setParameter("entityId", entityId);
        return executeQuerySelectFirst(q);
    }

    @Override
    public IndexFragmentEntryStatus findById(long entityId) {
        return this.getIndexFragmentEntryStatustByEntityId(entityId);
    }

    @Override
    public IndexFragmentEntryStatus getIndexFragmentEntryStatus(User user, UUID documentUUID) {
        TypedQuery<IndexFragmentEntryStatus> q = createTypedQuery("SELECT u FROM " + TABLENAME
                + " u WHERE u.userID = :userId and u.documentUUID = :documentUUID ORDER BY u.id ASC");
        q.setParameter("documentUUID", documentUUID);
        q.setParameter("userId", user.id());
        return executeQuerySelectFirst(q);
    }

    @Override
    public boolean isIndexFragmentEntryStatusExisting(User user, UUID documentUUID) {
        if (this.getIndexFragmentEntryStatus(user, documentUUID) == null) {
            return false;
        }
        return true;
    }

}
