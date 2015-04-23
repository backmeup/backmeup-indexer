package org.backmeup.index.rest.resources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.backmeup.index.api.SharingPolicyServer;
import org.backmeup.index.model.User;
import org.backmeup.index.model.sharing.SharingPolicyEntry;
import org.backmeup.index.model.sharing.SharingPolicyEntry.SharingPolicyTypeEntry;
import org.backmeup.index.sharing.policy.SharingPolicies;
import org.backmeup.index.sharing.policy.SharingPolicy;
import org.backmeup.index.sharing.policy.SharingPolicy2DocumentUUIDConverter;
import org.backmeup.index.sharing.policy.SharingPolicyManager;

@Path("sharing")
@Produces(MediaType.APPLICATION_JSON)
public class Sharing implements SharingPolicyServer {

    @Inject
    private SharingPolicyManager sharingManager;
    @Inject
    private SharingPolicy2DocumentUUIDConverter pol2uuidConverter;

    @Override
    @GET
    @Path("/{fromUserId}/owned")
    public Set<SharingPolicyEntry> getAllOwned(//
            @PathParam("fromUserId") User fromUser) {
        mandatory("fromUserId", fromUser);

        List<SharingPolicy> lp = this.sharingManager.getAllActivePoliciesOwnedByUser(fromUser);
        return convert(lp);
    }

    @Override
    @GET
    @Path("/{fromUserId}/incoming")
    public Set<SharingPolicyEntry> getAllIncoming(//
            @PathParam("fromUserId") User currUser) {
        mandatory("fromUserId", currUser);

        List<SharingPolicy> lp = this.sharingManager.getAllActivePoliciesSharedWithUser(currUser);
        return convert(lp);
    }

    @Override
    @POST
    @Path("/{fromUserId}")
    @Produces(MediaType.APPLICATION_JSON)
    public SharingPolicyEntry add( //
            @PathParam("fromUserId") User fromUser, // 
            @QueryParam("withUserId") User withUser, //
            @QueryParam("policyType") SharingPolicyTypeEntry policyType, //
            @QueryParam("policyValue") String policyValue,//
            @QueryParam("name") String name,//
            @QueryParam("description") String description) {

        mandatory("fromUserId", fromUser);
        mandatory("withUserId", withUser);
        mandatory("policyType", policyType);
        if (policyType == SharingPolicyTypeEntry.Backup) {
            mandatory("policyValue", policyValue);
        } else if (policyType == SharingPolicyTypeEntry.Document) {
            mandatoryUUID("policyValue", policyValue);
        } else if ((policyType == SharingPolicyTypeEntry.DocumentGroup)) {
            mandatoryListFromString("policyValue", policyValue);
        }

        SharingPolicies policy = convert(policyType);
        SharingPolicy p = this.sharingManager.createAndAddSharingPolicy(fromUser, withUser, policy, policyValue, name,
                description);
        return convert(p);
    }

    @DELETE
    @Path("/{fromUserId}")
    public Response removeRS(// 
            @PathParam("fromUserId") User owner, //
            @QueryParam("policyID") Long policyID) {

        mandatory("fromUserId", owner);
        mandatory("policyID", policyID);

        try {
            return status(Response.Status.OK, removeOwned(owner, policyID));
        } catch (IllegalArgumentException e) {
            return status(Response.Status.NOT_ACCEPTABLE, "failed to delete policy: " + policyID);
        }

    }

    @Override
    public String removeOwned(User owner, Long policyID) {
        this.sharingManager.removeSharingPolicy(policyID);
        return "policy removed";
    }

    @Override
    public String removeAllOwned(User owner) {
        int count = this.sharingManager.getAllActivePoliciesOwnedByUser(owner).size();
        this.sharingManager.removeAllSharingPoliciesForUser(owner);
        return count + " policies removed";
    }

    @DELETE
    @Path("/{fromUserId}/all")
    public Response removeAllOwnedRS( //
            @PathParam("fromUserId") User owner) {
        mandatory("fromUserId", owner);
        return status(Response.Status.OK, removeAllOwned(owner));
    }

    private void mandatory(String name, String value) {
        if (value == null || value.isEmpty()) {
            badRequestMissingParameter(name);
        }
    }

    private void mandatoryUUID(String name, String value) {
        if (value == null || value.isEmpty()) {
            badRequestMissingParameter(name);
        }
        try {
            UUID.fromString(value);
        } catch (Exception e) {
            throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST)
                            .entity(name + " parameter is malformed. Expecting UUID of syntax: "
                                    + UUID.randomUUID().toString()).build());
        }
    }

    private void mandatoryListFromString(String name, String value) {
        if (value == null || value.isEmpty()) {
            badRequestMissingParameter(name);
        }
        try {
            String[] sArr = value.substring(1, value.length() - 1).split(",\\s*");
            List<String> lArr = Arrays.asList(sArr);
            if (lArr.size() <= 1) {
                badRequestMalformedListOfUUIDsParameter(name);
            }
            //test sample on UUIDs
            for (int i = 0; i < lArr.size(); i++) {
                UUID.fromString(lArr.get(i));
            }
        } catch (Exception e) {
            badRequestMalformedListOfUUIDsParameter(name);
        }
    }

    private void mandatory(String name, User user) {
        if (user == null || user.id() == 0) {
            badRequestMissingParameter(name);
        }
    }

    private void mandatory(String name, Long l) {
        if (l == null || l == 0) {
            badRequestMissingParameter(name);
        }
    }

    private void mandatory(String name, SharingPolicyTypeEntry type) {
        if (type == null || type.toString().isEmpty()) {
            badRequestMissingParameter(name);
        }
    }

    private void badRequestMissingParameter(String name) {
        throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST). //
                entity(name + " parameter is mandatory"). //
                build());
    }

    private void badRequestMalformedListOfUUIDsParameter(String name) {
        List<UUID> l = new ArrayList<UUID>();
        l.add(UUID.randomUUID());
        l.add(UUID.randomUUID());
        throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST). //
                entity(name + " parameter is malformed. Expecting list in syntax: " + l.toString()). //
                build());
    }

    private Response status(Response.Status code, String message) {
        return Response.status(code).entity(message).build();
    }

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
        return null;
    }

    private SharingPolicyEntry convert(SharingPolicy p) {
        SharingPolicyTypeEntry t = convert(p.getPolicy());
        int polDocCount = this.pol2uuidConverter.getNumberOfDocsInPolicyForOwner(p);
        SharingPolicyEntry e = new SharingPolicyEntry(p.getId(), new User(p.getFromUserID()), new User(
                p.getWithUserID()), t, p.getPolicyCreationDate(), p.getSharedElementID(), p.getName(),
                p.getDescription(), polDocCount);
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
