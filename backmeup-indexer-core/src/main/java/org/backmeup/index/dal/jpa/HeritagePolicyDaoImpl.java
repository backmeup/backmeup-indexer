package org.backmeup.index.dal.jpa;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.backmeup.index.dal.HeritagePolicyDao;
import org.backmeup.index.dal.SharingPolicyDao;
import org.backmeup.index.model.User;
import org.backmeup.index.sharing.policy.SharingPolicy;
import org.backmeup.index.sharing.policy.SharingPolicy.ActivityState;
import org.backmeup.index.sharing.policy.SharingPolicy.Type;

@RequestScoped
public class HeritagePolicyDaoImpl extends BaseDaoImpl<SharingPolicy> implements HeritagePolicyDao {

    @Inject
    SharingPolicyDao sharingPolicyDao;

    public HeritagePolicyDaoImpl() {
        super(SharingPolicy.class);
    }

    @Override
    public SharingPolicy getByEntityId(Long entityId) {
        return this.sharingPolicyDao.getByEntityId(entityId);
    }

    @Override
    public List<SharingPolicy> getAllHeritagePolicies() {
        List<SharingPolicy> lPolicies = this.sharingPolicyDao.getAllSharingPolicies();
        return filterByHeritageType(lPolicies);
    }

    @Override
    public List<SharingPolicy> getAllHeritagePoliciesFromUser(User fromUser) {
        List<SharingPolicy> lPolicies = this.sharingPolicyDao.getAllSharingPoliciesFromUser(fromUser);
        return filterByHeritageType(lPolicies);
    }

    @Override
    public List<SharingPolicy> getAllHeritagePoliciesWithUser(User withUser) {
        List<SharingPolicy> lPolicies = this.sharingPolicyDao.getAllSharingPoliciesWithUser(withUser);
        return filterByHeritageType(lPolicies);
    }

    @Override
    public SharingPolicy getHeritagePolicyWithUserAndPolicyID(User withUser, Long policyID) {
        SharingPolicy policy = this.sharingPolicyDao.getAllSharingPoliciesWithUserAndPolicyID(withUser, policyID);
        if ((policy != null) && (policy.getType().equals(Type.HERITAGE))) {
            return policy;
        } else {
            return null;
        }
    }

    @Override
    public SharingPolicy getHeritagePolicyFromUserAndPolicyID(User fromUser, Long policyID) {
        SharingPolicy policy = this.sharingPolicyDao.getAllSharingPoliciesFromUserAndPolicyID(fromUser, policyID);
        if ((policy != null) && (policy.getType().equals(Type.HERITAGE))) {
            return policy;
        } else {
            return null;
        }
    }

    @Override
    public List<SharingPolicy> getAllHeritagePoliciesBetweenUsers(User fromUser, User withUser) {
        List<SharingPolicy> lPolicies = this.sharingPolicyDao.getAllSharingPoliciesBetweenUsers(fromUser, withUser);
        return filterByHeritageType(lPolicies);
    }

    @Override
    public void activateHeritage(User deadUser, User withUser) {
        List<SharingPolicy> lPolicies = getAllHeritagePoliciesBetweenUsers(deadUser, withUser);
        lPolicies = filterByHeritageActivityState(lPolicies);
        for (SharingPolicy policy : lPolicies) {
            policy.setState(ActivityState.ACCEPTED_AND_WAITING_FOR_TIMSPAN_START);
            this.sharingPolicyDao.merge(policy);
        }
    }

    private List<SharingPolicy> filterByHeritageType(List<SharingPolicy> policies) {
        List<SharingPolicy> ret = new ArrayList<SharingPolicy>();
        for (SharingPolicy policy : policies) {
            if (policy.getType().equals(Type.HERITAGE)) {
                ret.add(policy);
            }
        }
        return ret;
    }

    private List<SharingPolicy> filterByHeritageActivityState(List<SharingPolicy> policies) {
        List<SharingPolicy> ret = new ArrayList<SharingPolicy>();
        for (SharingPolicy policy : policies) {
            if (policy.getState().equals(ActivityState.HERITAGE_WAITING_FOR_ACTIVATION)) {
                ret.add(policy);
            }
        }
        return ret;
    }

}
