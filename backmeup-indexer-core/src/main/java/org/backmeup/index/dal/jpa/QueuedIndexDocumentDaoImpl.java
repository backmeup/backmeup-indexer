package org.backmeup.index.dal.jpa;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.persistence.TypedQuery;

import org.backmeup.index.core.model.QueuedIndexDocument;
import org.backmeup.index.dal.QueuedIndexDocumentDao;

@RequestScoped
public class QueuedIndexDocumentDaoImpl extends BaseDaoImpl<QueuedIndexDocument> implements QueuedIndexDocumentDao {

    private static final String TABLENAME = QueuedIndexDocument.class.getSimpleName();

    public QueuedIndexDocumentDaoImpl() {
        super(QueuedIndexDocument.class);
    }

    @Override
    public List<QueuedIndexDocument> getAllQueuedIndexDocuments() {
        TypedQuery<QueuedIndexDocument> q = createTypedQuery("SELECT u FROM " + TABLENAME
                + " u ORDER BY u.timestamp ASC");
        return executeQuery(q);
    }

    @Transactional
    @Override
    public void deleteAll() {
        this.entityManager.createQuery("DELETE FROM +" + TABLENAME).executeUpdate();
    }

    private TypedQuery<QueuedIndexDocument> createTypedQuery(String sql) {
        return this.entityManager.createQuery(sql, QueuedIndexDocument.class);
    }

    private List<QueuedIndexDocument> executeQuery(TypedQuery<QueuedIndexDocument> q) {
        List<QueuedIndexDocument> queuedIndexDocs = q.getResultList();
        if (queuedIndexDocs != null && queuedIndexDocs.size() > 0) {
            return queuedIndexDocs;
        }
        return new ArrayList<>();
    }

    private QueuedIndexDocument executeQuerySelectFirst(TypedQuery<QueuedIndexDocument> q) {
        List<QueuedIndexDocument> queuedIndexDocs = executeQuery(q);
        return queuedIndexDocs.size() > 0 ? queuedIndexDocs.get(0) : null;
    }

    @Override
    public QueuedIndexDocument findQueuedIndexDocumentByEntityId(Long entityId) {
        TypedQuery<QueuedIndexDocument> q = createTypedQuery("SELECT u FROM " + TABLENAME + " u WHERE u.Id = :entityId");
        q.setParameter("entityId", entityId);
        return executeQuerySelectFirst(q);
    }

    @Override
    public QueuedIndexDocument findById(long entityId) {
        return this.findQueuedIndexDocumentByEntityId(entityId);
    }

}
