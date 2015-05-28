package org.backmeup.index.dal.jpa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.persistence.TypedQuery;

import org.backmeup.index.dal.SharingPolicyDao;
import org.backmeup.index.model.User;
import org.backmeup.index.sharing.policy.SharingPolicies;
import org.backmeup.index.sharing.policy.SharingPolicy;
import org.backmeup.index.sharing.policy.SharingPolicy.ActivityState;

@RequestScoped
public class SharingPolicyDaoImpl extends BaseDaoImpl<SharingPolicy> implements SharingPolicyDao {

    private static final String TABLENAME = SharingPolicy.class.getSimpleName();

    public SharingPolicyDaoImpl() {
        super(SharingPolicy.class);
    }

    @Override
    public SharingPolicy getByEntityId(Long entityId) {
        TypedQuery<SharingPolicy> q = createTypedQuery("SELECT u FROM " + TABLENAME + " u WHERE u.Id = :entityId");
        q.setParameter("entityId", entityId);
        return executeQuerySelectLast(q);
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
    public List<SharingPolicy> getAllSharingPoliciesFromUserInState(User fromUser, ActivityState... state) {
        List<ActivityState> lState = Arrays.asList(state);
        TypedQuery<SharingPolicy> q = createTypedQuery("SELECT u FROM " + TABLENAME
                + " u WHERE u.fromUserID = :fromUserID and u.state IN (:activeState) ORDER BY u.Id ASC");
        q.setParameter("fromUserID", fromUser.id());
        q.setParameter("activeState", lState);
        return executeQuery(q);
    }

    @Override
    public List<SharingPolicy> getAllSharingPoliciesFromUserInStateAndOfType(User fromUser, SharingPolicies type,
            ActivityState... state) {
        List<ActivityState> lState = Arrays.asList(state);
        TypedQuery<SharingPolicy> q = createTypedQuery("SELECT u FROM "
                + TABLENAME
                + " u WHERE u.fromUserID = :fromUserID and u.policy = :policyType and u.state IN (:activeState) ORDER BY u.Id ASC");
        q.setParameter("fromUserID", fromUser.id());
        q.setParameter("activeState", lState);
        q.setParameter("policyType", type);
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
    public SharingPolicy getAllSharingPoliciesWithUserAndPolicyID(User withUser, Long policyID) {
        TypedQuery<SharingPolicy> q = createTypedQuery("SELECT u FROM " + TABLENAME
                + " u WHERE u.withUserID = :withUserID and u.Id = :policyId ORDER BY u.Id ASC");
        q.setParameter("withUserID", withUser.id());
        q.setParameter("policyId", policyID);
        return executeQuerySelectLast(q);
    }

    @Override
    public List<SharingPolicy> getAllSharingPoliciesWithUserInState(User withUser, ActivityState... state) {
        List<ActivityState> lState = Arrays.asList(state);
        TypedQuery<SharingPolicy> q = createTypedQuery("SELECT u FROM " + TABLENAME
                + " u WHERE u.withUserID = :withUserID and u.state IN (:activeState) ORDER BY u.Id ASC");
        q.setParameter("withUserID", withUser.id());
        q.setParameter("activeState", lState);
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

    @Override
    public List<SharingPolicy> getAllSharingPoliciesBetweenUserInState(User fromUser, User withUser,
            ActivityState... state) {
        List<ActivityState> lState = Arrays.asList(state);
        TypedQuery<SharingPolicy> q = createTypedQuery("SELECT u FROM "
                + TABLENAME
                + " u WHERE u.withUserID = :withUserID and u.fromUserID = :fromUserID and u.state IN (:activeState) ORDER BY u.Id ASC");
        q.setParameter("withUserID", withUser.id());
        q.setParameter("fromUserID", fromUser.id());
        q.setParameter("activeState", lState);
        return executeQuery(q);
    }

    @Override
    public List<SharingPolicy> getAllSharingPoliciesBetweenUsersInType(User fromUser, User withUser,
            SharingPolicies... types) {
        List<SharingPolicies> lTypes = Arrays.asList(types);
        TypedQuery<SharingPolicy> q = createTypedQuery("SELECT u FROM "
                + TABLENAME
                + " u WHERE u.withUserID = :withUserID and u.fromUserID = :fromUserID and u.policy IN (:statusTypes) ORDER BY u.Id ASC");
        q.setParameter("withUserID", withUser.id());
        q.setParameter("fromUserID", fromUser.id());
        q.setParameter("statusTypes", lTypes);
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

    private SharingPolicy executeQuerySelectLast(TypedQuery<SharingPolicy> q) {
        List<SharingPolicy> status = executeQuery(q);
        return status.size() > 0 ? status.get(status.size() - 1) : null;
    }

    @Transactional
    @Override
    public void deleteAll() {
        this.entityManager.createQuery("DELETE FROM +" + TABLENAME).executeUpdate();
    }

}
