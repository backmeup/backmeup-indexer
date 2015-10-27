package org.backmeup.index.rest.resources;

import java.io.IOException;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.backmeup.index.api.IndexerUserMappingServer;
import org.backmeup.index.dal.UserMappingHelperDao;
import org.backmeup.index.utils.file.UserMappingHelper;

@Path("user/mapping")
@Produces(MediaType.APPLICATION_JSON)
public class UserMapping extends ParameterValidator implements IndexerUserMappingServer {

    @Inject
    private UserMappingHelperDao userMappingDao;

    @Override
    public String updateUserMapping(Long bmuUserId, String keyserverUserId) throws IllegalArgumentException, IllegalStateException {
        UserMappingHelper umInput = new UserMappingHelper(bmuUserId, keyserverUserId);
        this.userMappingDao.save(umInput);
        UserMappingHelper umFound = this.userMappingDao.getByBMUUserId(umInput.getBmuUserId());
        if ((umFound != null) && (umFound.toString().equals(umInput.toString()))) {
            return "bmu/keyserver userID mapping successfully created";
        }
        //never reaches this block as non valid cases IllegalStateException: Transaction not active will be thrown
        return null;
    }

    @POST
    @Path("/add")
    public Response updateUserMappingRS(//
            @QueryParam("bmuuserid") Long bmuUserId, //
            @QueryParam("keyserveruserid") String keyserverUserId) {
        mandatory("bmuuserid", bmuUserId);
        mandatory("keyserveruserid", keyserverUserId);
        try {
            return status(Response.Status.OK, updateUserMapping(bmuUserId, keyserverUserId));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return status(Response.Status.NOT_ACCEPTABLE, "failed to create bmu/keyserver user mapping " + e);
        }
    }

    @GET
    @Path("/query")
    public Response getUserMappingRS(//
            @QueryParam("bmuuserid") Long bmuUserId, //
            @QueryParam("ksuserid") String keyserverUserId) {

        try {
            if (bmuUserId != null) {
                return status(Response.Status.OK, getKeyserverUserID(bmuUserId));
            } else if (keyserverUserId != null) {
                return status(Response.Status.OK, getBMUUserID(keyserverUserId) + "");
            }
        } catch (IOException e) {
            return status(Response.Status.NOT_ACCEPTABLE, "failed to get bmuUser/keyServerUser mapping " + e);
        }

        //in case both request parameters were empty
        throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                .entity("Parameter bmuuserid or ksuserid is required").build());

    }

    @Override
    public String getKeyserverUserID(Long bmuUserId) throws IOException {
        UserMappingHelper umFound = this.userMappingDao.getByBMUUserId(bmuUserId);
        if (umFound != null) {
            return umFound.getKsUserId();
        } else {
            throw new IOException("Item not found");
        }
    }

    @Override
    public Long getBMUUserID(String keyserverUserId) throws IOException {
        UserMappingHelper umFound = this.userMappingDao.getByKeyserverId(keyserverUserId);
        if (umFound != null) {
            return umFound.getBmuUserId();
        } else {
            throw new IOException("Item not found");
        }
    }
}
