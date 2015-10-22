package org.backmeup.index.dal.jpa;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.persistence.TypedQuery;

import org.backmeup.index.dal.UserMappingHelperDao;
import org.backmeup.index.utils.file.UserMappingHelper;

@RequestScoped
public class UserMappingHelperDaoImpl extends BaseDaoImpl<UserMappingHelper> implements UserMappingHelperDao {

    private static final String TABLENAME = UserMappingHelper.class.getSimpleName();

    public UserMappingHelperDaoImpl() {
        super(UserMappingHelper.class);
    }

    @Override
    public UserMappingHelper getByBMUUserId(Long bmuUserId) {
        TypedQuery<UserMappingHelper> q = createTypedQuery("SELECT u FROM " + TABLENAME + " u WHERE u.bmuUserId = :bmuUserId");
        q.setParameter("bmuUserId", bmuUserId);
        return executeQuerySelectFirst(q);
    }

    @Override
    public UserMappingHelper getByKeyserverId(String ksUserId) {
        TypedQuery<UserMappingHelper> q = createTypedQuery("SELECT u FROM " + TABLENAME + " u WHERE u.ksUserId = :ksUserId");
        q.setParameter("ksUserId", ksUserId);
        return executeQuerySelectFirst(q);
    }

    private TypedQuery<UserMappingHelper> createTypedQuery(String sql) {
        return this.entityManager.createQuery(sql, UserMappingHelper.class);
    }

    private List<UserMappingHelper> executeQuery(TypedQuery<UserMappingHelper> q) {
        List<UserMappingHelper> status = q.getResultList();
        if (status != null && status.size() > 0) {
            return status;
        }
        return new ArrayList<>();
    }

    private UserMappingHelper executeQuerySelectFirst(TypedQuery<UserMappingHelper> q) {
        List<UserMappingHelper> status = executeQuery(q);
        return status.size() > 0 ? status.get(0) : null;
    }

    @Transactional
    @Override
    public void deleteAll() {
        this.entityManager.createQuery("DELETE FROM +" + TABLENAME).executeUpdate();
    }

}
