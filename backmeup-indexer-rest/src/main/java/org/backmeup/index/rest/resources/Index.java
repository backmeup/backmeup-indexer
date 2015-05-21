package org.backmeup.index.rest.resources;

import java.io.IOException;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
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

import org.backmeup.index.api.IndexClient;
import org.backmeup.index.api.IndexServer;
import org.backmeup.index.model.FileInfo;
import org.backmeup.index.model.FileItem;
import org.backmeup.index.model.IndexDocument;
import org.backmeup.index.model.SearchResultAccumulator;
import org.backmeup.index.model.User;
import org.backmeup.index.query.ElasticSearchSetup;

@Path("index")
@Produces(MediaType.APPLICATION_JSON)
public class Index implements IndexServer {

    @Inject
    private ElasticSearchSetup clientFactory;

    private IndexClient getIndexClient(User userId) {
        return this.clientFactory.createIndexClient(userId);
    }

    @Override
    @GET
    @Path("/{userId}")
    public SearchResultAccumulator query( //
            @PathParam("userId") User userId, // 
            @QueryParam("query") String query, //
            @QueryParam("source") String filterBySource, //
            @QueryParam("type") String filterByType, //
            @QueryParam("job") String filterByJob, //
            @QueryParam("owner") String filterByOwner, //
            @QueryParam("tag") String filterByTag, //
            @QueryParam("username") String username) {
        mandatory("query", query);
        mandatory("username", username);

        try (IndexClient indexClient = getIndexClient(userId)) {

            return indexClient.queryBackup(query, filterBySource, filterByType, filterByJob, filterByOwner,
                    filterByTag, username);

        }
    }

    @Override
    @GET
    @Path("/{userId}/files")
    public Set<FileItem> filesForJob( //
            @PathParam("userId") User userId, // 
            @QueryParam("job") Long jobId) {
        mandatory("job", jobId);

        try (IndexClient indexClient = getIndexClient(userId)) {

            return indexClient.searchAllFileItemsForJob(jobId);

        }
    }

    @Override
    @GET
    @Path("/{userId}/files/{fileId}/info")
    public FileInfo fileInfoForFile( //
            @PathParam("userId") User userId, // 
            @PathParam("fileId") String fileId) {
        mandatory("fileId", fileId);

        try (IndexClient indexClient = getIndexClient(userId)) {

            return indexClient.getFileInfoForFile(fileId);

        }
    }

    @Override
    @GET
    @Path("/{userId}/files/{fileId}/thumbnail")
    public String thumbnailPathForFile( //
            @PathParam("userId") User userId, // 
            @PathParam("fileId") String fileId) {
        mandatory("fileId", fileId);

        try (IndexClient indexClient = getIndexClient(userId)) {

            return indexClient.getThumbnailPathForFile(fileId);

        }
    }

    @Override
    public String delete( //
            User userId, //
            Long jobId, //  
            Date timestamp) { // optional

        try (IndexClient indexClient = getIndexClient(userId)) {

            if (jobId != null && jobId != 0) {
                indexClient.deleteRecordsForUserAndJobAndTimestamp(jobId, timestamp);
                return "index records of job " + jobId + " and timestamp " + timestamp.toString() + " deleted for user";
            }

            indexClient.deleteRecordsForUser();
            return "all index records of user deleted";

        }
    }

    @Override
    public String delete(User userId, UUID indexFragmentUUID) {
        try (IndexClient indexClient = getIndexClient(userId)) {
            indexClient.deleteRecordsForUserAndDocumentUUID(indexFragmentUUID);
            return "index fragment with uuid " + indexFragmentUUID + " deleted for user";

        }
    }

    @DELETE
    @Path("/{userId}")
    public Response deleteRS( //
            @PathParam("userId") User userId, //
            @QueryParam("job") Long jobId, // delete either via user and timestamp OR
            @QueryParam("time") Long timestamp, //
            @QueryParam("document") UUID indexFragmentUUID) { //delete via document UUID

        //scenario1: delete record for user via job and timestamp
        if ((jobId != null) && (jobId != 0)) {
            mandatory("job", jobId);
            mandatory("time", timestamp);
            return status(Response.Status.ACCEPTED, delete(userId, jobId, new Date(timestamp)));
        }
        if ((timestamp != null) && (timestamp != 0)) {
            mandatory("job", jobId);
            mandatory("time", timestamp);
            return status(Response.Status.ACCEPTED, delete(userId, jobId, new Date(timestamp)));
        }

        //scenario2: delete record fors user via document uuid
        if (indexFragmentUUID != null) {
            mandatory("document", indexFragmentUUID);
            return status(Response.Status.ACCEPTED, delete(userId, indexFragmentUUID));
        }

        //scenario3: no parameters used, delete entire index for user
        return status(Response.Status.ACCEPTED, delete(userId, null, null));

    }

    @Override
    public String index( //
            User userId, //
            IndexDocument document) throws IOException {

        try (IndexClient indexClient = getIndexClient(userId)) {

            indexClient.index(document);
            return "document indexed";

        }
    }

    @POST
    @Path("/{userId}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response indexRS( //
            @PathParam("userId") User userId, //
            IndexDocument document) throws IOException {

        return status(Response.Status.CREATED, index(userId, document));
    }

    private void mandatory(String name, String value) {
        if (value == null || value.isEmpty()) {
            badRequestMissingParameter(name);
        }
    }

    private void mandatory(String name, UUID value) {
        if (value == null || value.toString().isEmpty()) {
            badRequestMissingParameter(name);
        }
    }

    private void mandatory(String name, Long value) {
        if (value == null || value == 0) {
            badRequestMissingParameter(name);
        }
    }

    private void badRequestMissingParameter(String name) {
        throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST). //
                entity(name + " parameter is mandatory"). //
                build());
    }

    private Response status(Response.Status code, String message) {
        return Response.status(code).entity(message).build();
    }

}
