package org.backmeup.index.dal.jpa;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.persistence.TypedQuery;

import org.backmeup.index.dal.SharingPolicyDao;
import org.backmeup.index.model.User;
import org.backmeup.index.sharing.policy.SharingPolicy;

@RequestScoped
public class SharingPolicyDaoImpl extends BaseDaoImpl<SharingPolicy> implements SharingPolicyDao {

    private static final String TABLENAME = SharingPolicy.class.getSimpleName();

    public SharingPolicyDaoImpl() {
        super(SharingPolicy.class);
    }

    @Transactional
    @Override
    public void deleteAll() {
        this.entityManager.createQuery("DELETE FROM +" + TABLENAME).executeUpdate();
    }

    @Override
    public SharingPolicy getByEntityId(Long entityId) {
        TypedQuery<SharingPolicy> q = createTypedQuery("SELECT u FROM " + TABLENAME + " u WHERE u.Id = :entityId");
        q.setParameter("entityId", entityId);
        return executeQuerySelectFirst(q);
    }

    @Override
    public List<SharingPolicy> getAllSharingPolicies() {
        TypedQuery<SharingPolicy> q = createTypedQuery("SELECT u FROM " + TABLENAME + " u ORDER BY u.Id ASC");
        return executeQuery(q);
    }

    @Override
    public List<SharingPolicy> getAllSharingPoliciesFromUser(User fromUser) {
        TypedQuery<SharingPolicy> q = createTypedQuery("SELECT u FROM " + TABLENAME
                + " u WHERE u.fromUserID = :fromUserID ORDER BY u.Id ASC");
        q.setParameter("fromUserID", fromUser.id());
        return executeQuery(q);
    }

    @Override
    public List<SharingPolicy> getAllSharingPoliciesWithUser(User withUser) {
        TypedQuery<SharingPolicy> q = createTypedQuery("SELECT u FROM " + TABLENAME
                + " u WHERE u.withUserID = :withUserID ORDER BY u.Id ASC");
        q.setParameter("withUserID", withUser.id());
        return executeQuery(q);
    }

    @Override
    public List<SharingPolicy> getAllSharingPoliciesBetweenUsers(User fromUser, User withUser) {
        TypedQuery<SharingPolicy> q = createTypedQuery("SELECT u FROM " + TABLENAME
                + " u WHERE u.withUserID = :withUserID and u.fromUserID = :fromUserID ORDER BY u.Id ASC");
        q.setParameter("withUserID", withUser.id());
        q.setParameter("fromUserID", fromUser.id());
        return executeQuery(q);
    }

    private TypedQuery<SharingPolicy> createTypedQuery(String sql) {
        return this.entityManager.createQuery(sql, SharingPolicy.class);
    }

    private List<SharingPolicy> executeQuery(TypedQuery<SharingPolicy> q) {
        List<SharingPolicy> status = q.getResultList();
        if (status != null && status.size() > 0) {
            return status;
        }
        return new ArrayList<>();
    }

    private SharingPolicy executeQuerySelectFirst(TypedQuery<SharingPolicy> q) {
        List<SharingPolicy> status = executeQuery(q);
        return status.size() > 0 ? status.get(0) : null;
    }

}
