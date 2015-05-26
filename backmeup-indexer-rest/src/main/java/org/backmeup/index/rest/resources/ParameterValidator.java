package org.backmeup.index.rest.resources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.backmeup.index.model.User;
import org.backmeup.index.model.sharing.SharingPolicyEntry.SharingPolicyTypeEntry;

public class ParameterValidator {

    //TODO AL refactoring Sharing, Index, etc. by extending this class

    public void mandatory(String name, User user) {
        if (user == null || user.id() == 0) {
            badRequestMissingParameter(name);
        }
    }

    public void mandatory(String name, Long l) {
        if (l == null || l == 0) {
            badRequestMissingParameter(name);
        }
    }

    public void mandatory(String name, String value) {
        if (value == null || value.isEmpty()) {
            badRequestMissingParameter(name);
        }
    }

    public void mandatoryLong(String name, String value) {
        try {
            Long l = Long.valueOf(value);
            mandatory(name, l);
        } catch (Exception e) {
            badRequestMissingParameter(name);
        }
    }

    public void mandatoryUUID(String name, String value) {
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

    public void mandatory(String name, List<UUID> value) {
        if (value == null || value.size() < 0) {
            badRequestMissingParameter(name);
        }
    }

    public void mandatoryListFromString(String name, String value) {
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

    public void mandatory(String name, SharingPolicyTypeEntry type) {
        if (type == null || type.toString().isEmpty()) {
            badRequestMissingParameter(name);
        }
    }

    public void badRequestMalformedListOfUUIDsParameter(String name) {
        List<UUID> l = new ArrayList<UUID>();
        l.add(UUID.randomUUID());
        l.add(UUID.randomUUID());
        throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST). //
                entity(name + " parameter is malformed. Expecting list in syntax: " + l.toString()). //
                build());
    }

    public Response status(Response.Status code, String message) {
        return Response.status(code).entity(message).build();
    }

    public void badRequestMissingParameter(String name) {
        throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST). //
                entity(name + " parameter is mandatory"). //
                build());
    }

}
