package org.backmeup.index.rest.resources;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.backmeup.index.api.SharingPolicyServer;
import org.backmeup.index.model.User;
import org.backmeup.index.model.sharing.SharingPolicyEntry;
import org.backmeup.index.model.sharing.SharingPolicyEntry.SharingPolicyTypeEntry;
import org.backmeup.index.rest.utils.DateFormat;
import org.backmeup.index.sharing.policy.SharingPolicies;
import org.backmeup.index.sharing.policy.SharingPolicy;
import org.backmeup.index.sharing.policy.SharingPolicy.ActivityState;
import org.backmeup.index.sharing.policy.SharingPolicy.Type;
import org.backmeup.index.sharing.policy.SharingPolicy2DocumentUUIDConverter;
import org.backmeup.index.sharing.policy.SharingPolicyManager;

@Path("sharing")
@Produces(MediaType.APPLICATION_JSON)
public class Sharing extends ParameterValidator implements SharingPolicyServer {

    @Inject
    private SharingPolicyManager sharingManager;
    @Inject
    private SharingPolicy2DocumentUUIDConverter pol2uuidConverter;

    @Override
    @GET
    @Path("/{fromUserId}/owned")
    public Set<SharingPolicyEntry> getAllOwnedSharingPolicies(//
            @PathParam("fromUserId") User fromUser) {
        mandatory("fromUserId", fromUser);

        List<SharingPolicy> lp = this.sharingManager
                .getAllWaiting4HandshakeAndScheduledAndActivePoliciesOwnedByUser(fromUser);
        return convert(lp);
    }

    @Override
    @GET
    @Path("/{fromUserId}/incoming")
    public Set<SharingPolicyEntry> getAllIncomingSharingPolicies(//
            @PathParam("fromUserId") User currUser) {
        mandatory("fromUserId", currUser);

        List<SharingPolicy> lp = this.sharingManager
                .getAllWaiting4HandshakeAndScheduledAndActivePoliciesSharedWithUser(currUser);
        return convert(lp);
    }

    @Override
    @POST
    @Path("/{fromUserId}")
    @Produces(MediaType.APPLICATION_JSON)
    public SharingPolicyEntry addSharingPolicy( //
            @PathParam("fromUserId") User fromUser, // 
            @QueryParam("withUserId") User withUser, //
            @QueryParam("policyType") SharingPolicyTypeEntry policyType, //
            @QueryParam("policyValue") String policyValue,//
            @QueryParam("name") String name,//
            @QueryParam("description") String description,//
            @QueryParam("lifespanstart") @DateFormat("EE MMM dd hh:mm:ss z yyyy") Date lifespanStart,//
            @QueryParam("lifespanend") @DateFormat("EE MMM dd hh:mm:ss z yyyy") Date lifespanEnd) {

        SharingPolicy p = createAndAddPolicy(fromUser, withUser, policyType, policyValue, name, description,
                lifespanStart, lifespanEnd, Type.SHARING);
        return convert(p);
    }

    /**
     * Helper to reuse logic for both Heritage and standard Sharing policies
     * 
     * @return
     */
    private SharingPolicy createAndAddPolicy(User fromUser, User withUser, SharingPolicyTypeEntry policyType,
            String policyValue, String name, String description, Date lifespanStart, Date lifespanEnd, Type t) {

        mandatory("fromUserId", fromUser);
        mandatory("withUserId", withUser);
        mandatory("policyType", policyType);
        if (policyType == SharingPolicyTypeEntry.Backup) {
            mandatoryLong("policyValue", policyValue);
        } else if (policyType == SharingPolicyTypeEntry.Document) {
            mandatoryUUID("policyValue", policyValue);
        } else if ((policyType == SharingPolicyTypeEntry.DocumentGroup)) {
            mandatoryListFromString("policyValue", policyValue);
        } else if ((policyType == SharingPolicyTypeEntry.TaggedCollection)) {
            mandatoryLong("policyValue", policyValue);
        }

        SharingPolicies policy = convert(policyType);

        //distinguish between Sharing and Heritage policy
        SharingPolicy p = null;
        if (t.equals(Type.SHARING)) {
            p = this.sharingManager.createAndAddSharingPolicy(fromUser, withUser, policy, policyValue, name,
                    description, lifespanStart, lifespanEnd);
        }
        if (t.equals(Type.HERITAGE)) {
            p = this.sharingManager.createAndAddHeritagePolicy(fromUser, withUser, policy, policyValue, name,
                    description, lifespanStart, lifespanEnd);
        }
        return p;

    }

    @Override
    @POST
    @Path("/{fromUserId}/update")
    @Produces(MediaType.APPLICATION_JSON)
    public SharingPolicyEntry updateSharingPolicy(//
            @PathParam("fromUserId") User owner,//
            @QueryParam("policyID") Long policyID,//
            @QueryParam("name") String name,//
            @QueryParam("description") String description,//
            @QueryParam("lifespanstart") @DateFormat("EE MMM dd hh:mm:ss z yyyy") Date lifespanStart,//
            @QueryParam("lifespanend") @DateFormat("EE MMM dd hh:mm:ss z yyyy") Date lifespanEnd) {

        mandatory("fromUserId", owner);
        mandatory("policyID", policyID);
        SharingPolicy p = this.sharingManager.updateSharingPolicy(owner, policyID, name, description, lifespanStart,
                lifespanEnd);
        return convert(p);
    }

    @DELETE
    @Path("/{fromUserId}")
    public Response removeOwnedSharingPolicyRS(// 
            @PathParam("fromUserId") User owner, //
            @QueryParam("policyID") Long policyID) {

        mandatory("fromUserId", owner);
        mandatory("policyID", policyID);

        try {
            return status(Response.Status.OK, removeOwnedSharingPolicy(owner, policyID));
        } catch (IllegalArgumentException e) {
            return status(Response.Status.NOT_ACCEPTABLE, "failed to delete policy: " + policyID);
        }

    }

    @Override
    public String removeOwnedSharingPolicy(User owner, Long policyID) {
        this.sharingManager.removeSharingPolicy(policyID);
        return "policy removed";
    }

    @DELETE
    @Path("/{fromUserId}/all")
    public Response removeAllOwnedSharingPoliciesRS( //
            @PathParam("fromUserId") User owner) {
        mandatory("fromUserId", owner);
        return status(Response.Status.OK, removeAllOwnedSharingPolicies(owner));
    }

    @Override
    public String removeAllOwnedSharingPolicies(User owner) {
        int count = this.sharingManager.getAllWaiting4HandshakeAndScheduledAndActivePoliciesOwnedByUser(owner).size();
        this.sharingManager.removeAllSharingPoliciesForUser(owner);
        return count + " policies removed";
    }

    @POST
    @Path("/{currUserId}/acceptIncoming")
    public Response acceptIncomingSharingRS(//
            @PathParam("currUserId") User user, //
            @QueryParam("policyID") Long policyID) {

        mandatory("currUserId", user);
        mandatory("policyID", policyID);

        try {
            return status(Response.Status.OK, acceptIncomingSharing(user, policyID));
        } catch (IllegalArgumentException e) {
            return status(Response.Status.NOT_ACCEPTABLE, "failed to approve incoming sharing");
        }
    }

    @POST
    @Path("/{currUserId}/declineIncoming")
    public Response declineIncomingSharingRS(// 
            @PathParam("currUserId") User user, //
            @QueryParam("policyID") Long policyID) {

        mandatory("currUserId", user);
        mandatory("policyID", policyID);

        try {
            return status(Response.Status.OK, declineIncomingSharing(user, policyID));
        } catch (IllegalArgumentException e) {
            return status(Response.Status.NOT_ACCEPTABLE, "failed to decline incoming sharing");
        }
    }

    @Override
    public String acceptIncomingSharing(User user, Long policyID) {
        this.sharingManager.approveIncomingSharing(user, policyID);
        return "incoming sharing accepted";
    }

    @Override
    public String declineIncomingSharing(User user, Long policyID) {
        this.sharingManager.declineIncomingSharing(user, policyID);
        return "incoming sharing declined";
    }

    //-----------------------HERITAGE SHARING -------------------------//

    @Override
    @GET
    @Path("/heritage/{fromUserId}/owned")
    public Set<SharingPolicyEntry> getAllOwnedHeritagePolicies(//
            @PathParam("fromUserId") User fromUser) {
        mandatory("fromUserId", fromUser);
        List<SharingPolicy> lp = this.sharingManager.getAllHeritagePoliciesOwnedByUser(fromUser);
        return convert(lp);
    }

    @Override
    @GET
    @Path("/heritage/{fromUserId}/incoming")
    public Set<SharingPolicyEntry> getAllIncomingHeritagePolicies(//
            @PathParam("fromUserId") User currUser) {
        mandatory("fromUserId", currUser);
        List<SharingPolicy> lp = this.sharingManager.getAllHeritagePoliciesSharedWithUser(currUser);
        return convert(lp);
    }

    @Override
    @POST
    @Path("/heritage/{fromUserId}")
    @Produces(MediaType.APPLICATION_JSON)
    public SharingPolicyEntry addHeritagePolicy( //
            @PathParam("fromUserId") User fromUser, // 
            @QueryParam("withUserId") User withUser, //
            @QueryParam("policyType") SharingPolicyTypeEntry policyType, //
            @QueryParam("policyValue") String policyValue,//
            @QueryParam("name") String name,//
            @QueryParam("description") String description,//
            @QueryParam("lifespanstart") @DateFormat("EE MMM dd hh:mm:ss z yyyy") Date lifespanStart,//
            @QueryParam("lifespanend") @DateFormat("EE MMM dd hh:mm:ss z yyyy") Date lifespanEnd) {

        SharingPolicy p = createAndAddPolicy(fromUser, withUser, policyType, policyValue, name, description,
                lifespanStart, lifespanEnd, Type.HERITAGE);
        return convert(p);

    }

    @Override
    @POST
    @Path("/heritage/{fromUserId}/update")
    @Produces(MediaType.APPLICATION_JSON)
    public SharingPolicyEntry updateHeritagePolicy(//
            @PathParam("fromUserId") User owner,//
            @QueryParam("policyID") Long policyID,//
            @QueryParam("name") String name,//
            @QueryParam("description") String description,//
            @QueryParam("lifespanstart") @DateFormat("EE MMM dd hh:mm:ss z yyyy") Date lifespanStart,//
            @QueryParam("lifespanend") @DateFormat("EE MMM dd hh:mm:ss z yyyy") Date lifespanEnd) {

        mandatory("fromUserId", owner);
        mandatory("policyID", policyID);
        SharingPolicy p = this.sharingManager.updateHeritagePolicy(owner, policyID, name, description, lifespanStart,
                lifespanEnd);
        return convert(p);
    }

    @DELETE
    @Path("/heritage/{fromUserId}")
    public Response removeOwnedHeritagePolicyRS(// 
            @PathParam("fromUserId") User owner, //
            @QueryParam("policyID") Long policyID) {

        mandatory("fromUserId", owner);
        mandatory("policyID", policyID);

        try {
            return status(Response.Status.OK, removeOwnedHeritagePolicy(owner, policyID));
        } catch (IllegalArgumentException e) {
            return status(Response.Status.NOT_ACCEPTABLE, "failed to delete policy: " + policyID);
        }

    }

    @Override
    public String removeOwnedHeritagePolicy(User owner, Long policyID) {
        this.sharingManager.removeHeritagePolicy(policyID);
        return "policy removed";
    }

    @POST
    @Path("/heritage/{fromUserId}/deadmannswitch/activate")
    @Produces(MediaType.APPLICATION_JSON)
    public Response activateDeadMannSwitchAndImportRS(//
            @PathParam("fromUserId") User currUser) {

        try {
            return status(Response.Status.OK, this.activateDeadMannSwitchAndImport(currUser));
        } catch (IllegalArgumentException e) {
            return status(Response.Status.NOT_ACCEPTABLE, "failed to activate heritage for userId: " + currUser);
        }
    }

    @Override
    public String activateDeadMannSwitchAndImport(User currUser) {
        this.sharingManager.activateDeadManSwitchAndImport(currUser);
        return "activated heritage import";
    }

    //-----------------------conversion helper ---------------------------//

    private SharingPolicies convert(SharingPolicyTypeEntry policyType) {
        if (policyType == SharingPolicyTypeEntry.Backup) {
            return SharingPolicies.SHARE_BACKUP;
        }
        if (policyType == SharingPolicyTypeEntry.Document) {
            return SharingPolicies.SHARE_INDEX_DOCUMENT;
        }
        if (policyType == SharingPolicyTypeEntry.AllFromNow) {
            return SharingPolicies.SHARE_ALL_AFTER_NOW;
        }
        if (policyType == SharingPolicyTypeEntry.AllInklOld) {
            return SharingPolicies.SHARE_ALL_INKLUDING_OLD;
        }
        if (policyType == SharingPolicyTypeEntry.DocumentGroup) {
            return SharingPolicies.SHARE_INDEX_DOCUMENT_GROUP;
        }
        if (policyType == SharingPolicyTypeEntry.TaggedCollection) {
            return SharingPolicies.SHARE_TAGGED_COLLECTION;
        }
        return null;
    }

    private SharingPolicyTypeEntry convert(SharingPolicies policy) {
        if (policy == SharingPolicies.SHARE_BACKUP) {
            return SharingPolicyTypeEntry.Backup;
        }
        if (policy == SharingPolicies.SHARE_INDEX_DOCUMENT) {
            return SharingPolicyTypeEntry.Document;
        }
        if (policy == SharingPolicies.SHARE_ALL_AFTER_NOW) {
            return SharingPolicyTypeEntry.AllFromNow;
        }
        if (policy == SharingPolicies.SHARE_ALL_INKLUDING_OLD) {
            return SharingPolicyTypeEntry.AllInklOld;
        }
        if (policy == SharingPolicies.SHARE_INDEX_DOCUMENT_GROUP) {
            return SharingPolicyTypeEntry.DocumentGroup;
        }
        if (policy == SharingPolicies.SHARE_TAGGED_COLLECTION) {
            return SharingPolicyTypeEntry.TaggedCollection;
        }
        return null;
    }

    private SharingPolicyEntry convert(SharingPolicy p) {
        SharingPolicyTypeEntry t = convert(p.getPolicy());
        int polDocCount = this.pol2uuidConverter.getNumberOfDocsInPolicyForOwner(p);
        boolean incomingSharingAccepted = false;
        if (p.getState().equals(ActivityState.ACCEPTED_AND_ACTIVE)
                || p.getState().equals(ActivityState.ACCEPTED_AND_WAITING_FOR_TIMSPAN_START)) {
            incomingSharingAccepted = true;
        }

        SharingPolicyEntry e = new SharingPolicyEntry(p.getId(), new User(p.getFromUserID()), new User(
                p.getWithUserID()), t, p.getPolicyCreationDate(), p.getSharedElementID(), p.getName(),
                p.getDescription(), polDocCount, incomingSharingAccepted, p.getPolicyLifeSpanStartDate(),
                p.getPolicyLifeSpanEndDate());
        return e;
    }

    private Set<SharingPolicyEntry> convert(List<SharingPolicy> lp) {
        Set<SharingPolicyEntry> ret = new HashSet<SharingPolicyEntry>();

        for (SharingPolicy p : lp) {
            SharingPolicyEntry e = convert(p);
            ret.add(e);
        }
        return ret;
    }

}
