package org.backmeup.index.dal.jpa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
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
    public List<IndexFragmentEntryStatus> getAllFromUser(User user) {
        TypedQuery<IndexFragmentEntryStatus> q = createTypedQuery("SELECT u FROM " + TABLENAME
                + " u WHERE u.userID = :userId ORDER BY u.id ASC");
        q.setParameter("userId", user.id());
        return executeQuery(q);
    }

    @Override
    public List<IndexFragmentEntryStatus> getAllByDocumentUUID(UUID documentUUID) {
        TypedQuery<IndexFragmentEntryStatus> q = createTypedQuery("SELECT u FROM " + TABLENAME
                + " u WHERE u.documentUUID = :documentUUID ORDER BY u.id ASC");
        q.setParameter("documentUUID", documentUUID);
        return executeQuery(q);
    }

    @Override
    public List<IndexFragmentEntryStatus> getAllByStatusType(StatusType type) {
        TypedQuery<IndexFragmentEntryStatus> q = createTypedQuery("SELECT u FROM " + TABLENAME
                + " u WHERE u.statusType = :statusType ORDER BY u.id ASC");
        q.setParameter("statusType", type);
        return executeQuery(q);
    }

    @Override
    public List<IndexFragmentEntryStatus> getAllFromUserOfType(User user, StatusType type) {
        TypedQuery<IndexFragmentEntryStatus> q = createTypedQuery("SELECT u FROM " + TABLENAME
                + " u WHERE u.userID = :userId and u.statusType = :statusType ORDER BY u.id ASC");
        q.setParameter("userId", user.id());
        q.setParameter("statusType", type);
        return executeQuery(q);
    }

    @Override
    public List<IndexFragmentEntryStatus> getAllFromUserInOneOfTheTypes(User user, StatusType... types) {
        List<StatusType> lTypes = Arrays.asList(types);
        TypedQuery<IndexFragmentEntryStatus> q = createTypedQuery("SELECT u FROM " + TABLENAME
                + " u WHERE u.userID = :userId and u.statusType IN (:statusTypes) ORDER BY u.id ASC");
        q.setParameter("userId", user.id());
        q.setParameter("statusTypes", lTypes);
        return executeQuery(q);
    }

    @Override
    public List<IndexFragmentEntryStatus> getAllFromUserInOneOfTheTypesAndByDocumentOwner(User user,
            User actualDocumentOwner, StatusType... types) {
        List<StatusType> lTypes = Arrays.asList(types);
        TypedQuery<IndexFragmentEntryStatus> q = createTypedQuery("SELECT u FROM "
                + TABLENAME
                + " u WHERE u.userID = :userId and u.ownerID = :ownerId and u.statusType IN (:statusTypes) ORDER BY u.id ASC");
        q.setParameter("userId", user.id());
        q.setParameter("ownerId", actualDocumentOwner.id());
        q.setParameter("statusTypes", lTypes);
        return executeQuery(q);
    }

    @Override
    public List<IndexFragmentEntryStatus> getAllFromUserInOneOfTheTypesAndByUserAsDocumentOwner(User userAndOwner,
            StatusType... types) {
        return this.getAllFromUserInOneOfTheTypesAndByDocumentOwner(userAndOwner, userAndOwner, types);
    }

    @Override
    public IndexFragmentEntryStatus getByEntityId(Long entityId) {
        TypedQuery<IndexFragmentEntryStatus> q = createTypedQuery("SELECT u FROM " + TABLENAME
                + " u WHERE u.Id = :entityId");
        q.setParameter("entityId", entityId);
        return executeQuerySelectFirst(q);
    }

    @Override
    public IndexFragmentEntryStatus findById(long entityId) {
        return this.getByEntityId(entityId);
    }

    @Override
    public IndexFragmentEntryStatus getByUserAndDocumentUUID(User user, UUID documentUUID) {
        TypedQuery<IndexFragmentEntryStatus> q = createTypedQuery("SELECT u FROM " + TABLENAME
                + " u WHERE u.userID = :userId and u.documentUUID = :documentUUID ORDER BY u.id ASC");
        q.setParameter("documentUUID", documentUUID);
        q.setParameter("userId", user.id());
        return executeQuerySelectFirst(q);
    }

    @Override
    public boolean isIndexFragmentEntryStatusExisting(User user, UUID documentUUID) {
        if (this.getByUserAndDocumentUUID(user, documentUUID) == null) {
            return false;
        }
        return true;
    }

    @Override
    public List<IndexFragmentEntryStatus> getAllByUserAndBackupJobID(User user, long backupJobId) {
        TypedQuery<IndexFragmentEntryStatus> q = createTypedQuery("SELECT u FROM " + TABLENAME
                + " u WHERE u.userID = :userId and u.jobID = :backupJobId ORDER BY u.id ASC");
        q.setParameter("userId", user.id());
        q.setParameter("backupJobId", backupJobId);
        return executeQuery(q);
    }

    @Override
    public List<IndexFragmentEntryStatus> getAllByUserAndBeforeBackupDate(User user, Date backupDate) {
        TypedQuery<IndexFragmentEntryStatus> q = createTypedQuery("SELECT u FROM " + TABLENAME
                + " u WHERE u.userID = :userId and u.backupedAt <= :backupDate ORDER BY u.id ASC");
        q.setParameter("userId", user.id());
        q.setParameter("backupDate", backupDate);
        return executeQuery(q);
    }

    @Override
    public List<IndexFragmentEntryStatus> getAllByUserOwnedAndBeforeBackupDate(User user, Date date,
            StatusType... types) {
        return this.getAllByUserAndBeforeBackupDateAndByDocumentOwner(user, user, date, types);
    }

    @Override
    public List<IndexFragmentEntryStatus> getAllByUserAndBeforeBackupDateAndByDocumentOwner(User user,
            User actualDocOwner, Date date, StatusType... types) {
        List<StatusType> lTypes = Arrays.asList(types);
        TypedQuery<IndexFragmentEntryStatus> q = createTypedQuery("SELECT u FROM "
                + TABLENAME
                + " u WHERE u.userID = :userId and u.ownerID = :ownerId and u.statusType IN (:statusTypes) and u.backupedAt <= :date ORDER BY u.id ASC");
        q.setParameter("userId", user.id());
        q.setParameter("ownerId", actualDocOwner.id());
        q.setParameter("date", date);
        q.setParameter("statusTypes", lTypes);
        return executeQuery(q);
    }

    @Override
    public List<IndexFragmentEntryStatus> getAllByUserOwnedAndAfterBackupDate(User user, Date date, StatusType... types) {
        return this.getAllByUserAndAfterBackupDateAndByDocumentOwner(user, user, date, types);
    }

    @Override
    public List<IndexFragmentEntryStatus> getAllByUserAndAfterBackupDateAndByDocumentOwner(User user,
            User actualDocOwner, Date date, StatusType... types) {
        List<StatusType> lTypes = Arrays.asList(types);
        TypedQuery<IndexFragmentEntryStatus> q = createTypedQuery("SELECT u FROM "
                + TABLENAME
                + " u WHERE u.userID = :userId and u.ownerID = :ownerId and u.statusType IN (:statusTypes) and u.backupedAt > :date ORDER BY u.id ASC");
        q.setParameter("userId", user.id());
        q.setParameter("ownerId", actualDocOwner.id());
        q.setParameter("date", date);
        q.setParameter("statusTypes", lTypes);
        return executeQuery(q);
    }

    @Override
    public List<IndexFragmentEntryStatus> getAllByUserAndAfterBackupDate(User user, Date backupDate) {
        TypedQuery<IndexFragmentEntryStatus> q = createTypedQuery("SELECT u FROM " + TABLENAME
                + " u WHERE u.userID = :userId and u.backupedAt > :backupDate ORDER BY u.id ASC");
        q.setParameter("userId", user.id());
        q.setParameter("backupDate", backupDate);
        return executeQuery(q);
    }

    @Override
    public List<IndexFragmentEntryStatus> getAllByUserOwnedAndBackupJob(User user, Long backupJobID,
            StatusType... types) {
        return getAllByUserAndBackupJobAndByDocumentOwner(user, user, backupJobID, types);
    }

    @Override
    public List<IndexFragmentEntryStatus> getAllByUserAndBackupJobAndByDocumentOwner(User user, User actualDocOwner,
            Long backupJobID, StatusType... types) {
        List<StatusType> lTypes = Arrays.asList(types);
        TypedQuery<IndexFragmentEntryStatus> q = createTypedQuery("SELECT u FROM "
                + TABLENAME
                + " u WHERE u.userID = :userId and u.ownerID = :ownerId and u.statusType IN (:statusTypes) and u.jobID = :backupjobId ORDER BY u.id ASC");
        q.setParameter("userId", user.id());
        q.setParameter("ownerId", actualDocOwner.id());
        q.setParameter("backupjobId", backupJobID);
        q.setParameter("statusTypes", lTypes);
        return executeQuery(q);
    }

    @Override
    public IndexFragmentEntryStatus getByUserOwnedAndDocumentUUID(User user, UUID documentUUID, StatusType... types) {
        return this.getByUserAndDocumentUUIDByDocumentOwner(user, user, documentUUID, types);
    }

    @Override
    public IndexFragmentEntryStatus getByUserAndDocumentUUIDByDocumentOwner(User user, User actualDocOwner,
            UUID documentUUID, StatusType... types) {
        List<StatusType> lTypes = Arrays.asList(types);
        TypedQuery<IndexFragmentEntryStatus> q = createTypedQuery("SELECT u FROM "
                + TABLENAME
                + " u WHERE u.userID = :userId and u.ownerID = :ownerId and u.statusType IN (:statusTypes) and u.documentUUID = :docUUID ORDER BY u.id ASC");
        q.setParameter("userId", user.id());
        q.setParameter("ownerId", actualDocOwner.id());
        q.setParameter("docUUID", documentUUID);
        q.setParameter("statusTypes", lTypes);
        return executeQuerySelectFirst(q);
    }

}
