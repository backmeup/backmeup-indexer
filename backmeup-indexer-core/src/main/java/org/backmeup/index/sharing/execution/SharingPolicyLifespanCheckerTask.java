package org.backmeup.index.sharing.execution;

import java.util.Date;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.backmeup.index.dal.SharingPolicyDao;
import org.backmeup.index.sharing.policy.SharingPolicy;
import org.backmeup.index.sharing.policy.SharingPolicy.ActivityState;
import org.backmeup.index.utils.cdi.RunRequestScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Check on the given lifespan of a sharing policy. activates policies which should become active based on the
 * activation date a user selected for a given policy and discards a policy after the end of life date is reached
 *
 */
@ApplicationScoped
public class SharingPolicyLifespanCheckerTask implements Runnable {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    private SharingPolicyDao sharingPolicyDao;

    @Override
    @RunRequestScoped
    public void run() {
        checkExistingPolicies();
    }

    /**
     * Checks if a policy reached the boundaries of its lifespan, e.g. if a policy has set an activation date which was
     * now reached or if a policy timed out and should be deleted
     */
    private void checkExistingPolicies() {
        checkLifespanForPolicyActivations();
        checkLifespanForPolicyDeletions();
    }

    private void checkLifespanForPolicyActivations() {
        //get a list of all policies that have been handshaked but not activated and check on their activation date
        List<SharingPolicy> lPolicies = this.sharingPolicyDao
                .getAllSharingPoliciesOverAllUsersInState(ActivityState.ACCEPTED_AND_WAITING_FOR_TIMSPAN_START);

        Date dNow = new Date();
        for (SharingPolicy p : lPolicies) {
            Date pStartDate = p.getPolicyLifeSpanStartDate();
            //check on the policies lifespan date. compare will deliver 0 when equal and <0 when pStartDate before dNow
            if (pStartDate.compareTo(dNow) <= 0) {
                //we found a policy that should be activated based on the timespan selected by the user
                p.setState(ActivityState.ACCEPTED_AND_ACTIVE);
                this.sharingPolicyDao.merge(p);
                this.log.debug("marked SharingPolicy " + p.getId()
                        + " for activation based on selected timespan of the policy");
            }
        }
    }

    private void checkLifespanForPolicyDeletions() {
        //get a list of all active policies and check on their end of life timespan date
        List<SharingPolicy> lPolicies = this.sharingPolicyDao
                .getAllSharingPoliciesOverAllUsersInState(ActivityState.ACCEPTED_AND_ACTIVE);

        Date dNow = new Date();
        for (SharingPolicy p : lPolicies) {
            Date pEndDate = p.getPolicyLifeSpanEndDate();
            //check on the policies lifespan end date. compare will deliver 0 when equal and >0 when pEndDate after dNow
            if (dNow.compareTo(pEndDate) > 0) {
                //we found a policy that should no longer be active based on the timespan selected by the user
                p.setState(ActivityState.WAITING_FOR_DELETION);
                this.sharingPolicyDao.merge(p);
                this.log.debug("marked SharingPolicy " + p.getId()
                        + " for deletion based on selected timespan of the policy");
            }
        }
    }

}
