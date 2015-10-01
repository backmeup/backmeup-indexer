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
import org.backmeup.keyserver.model.dto.TokenDTO;

@Path("index")
@Produces(MediaType.APPLICATION_JSON)
public class Index implements IndexServer {

    @Inject
    private ElasticSearchSetup clientFactory;

    private IndexClient getIndexClient(User user) {
        return this.clientFactory.createIndexClient(user);
    }

    @GET
    @Path("/{userId}")
    public SearchResultAccumulator queryRS( //
            @PathParam("userId") Long userId, // 
            @QueryParam("query") String query, //
            @QueryParam("source") String filterBySource, //
            @QueryParam("type") String filterByType, //
            @QueryParam("job") String filterByJob, //
            @QueryParam("owner") String filterByOwner, //
            @QueryParam("tag") String filterByTag, //
            @QueryParam("username") String username,//
            @QueryParam("offset") Long offSetStart, //
            @QueryParam("maxresults") Long maxResults,//
            @QueryParam("kstoken") String kstoken) {
        mandatory("query", query);
        mandatory("username", username);
        mandatory("kstoken", kstoken);

        User user = new User(userId, TokenDTO.fromTokenString(kstoken));
        return this.query(user, query, filterBySource, filterByType, filterByJob, filterByOwner, filterByTag, username, offSetStart,
                maxResults);

    }

    @Override
    public SearchResultAccumulator query(User user, String query, String filterBySource, String filterByType, String filterByJob,
            String filterByOwner, String filterByTag, String username, Long queryOffSetStart, Long queryMaxResults) {
        try (IndexClient indexClient = getIndexClient(user)) {
            return indexClient.queryBackup(query, filterBySource, filterByType, filterByJob, filterByOwner, filterByTag, username,
                    queryOffSetStart, queryMaxResults);
        }
    }

    @GET
    @Path("/{userId}/files")
    public Set<FileItem> filesForJobRS( //
            @PathParam("userId") Long userId, // 
            @QueryParam("job") Long jobId,//
            @QueryParam("kstoken") String kstoken) {
        mandatory("job", jobId);
        mandatory("kstoken", kstoken);

        User user = new User(userId, TokenDTO.fromTokenString(kstoken));
        return this.filesForJob(user, jobId);

    }

    @Override
    public Set<FileItem> filesForJob(User user, Long jobId) {
        try (IndexClient indexClient = getIndexClient(user)) {
            return indexClient.searchAllFileItemsForJob(jobId);
        }
    }

    @GET
    @Path("/{userId}/files/{fileId}/info")
    public FileInfo fileInfoForFile( //
            @PathParam("userId") Long userId, // 
            @PathParam("fileId") String fileId, //
            @QueryParam("kstoken") String kstoken) {
        mandatory("fileId", fileId);
        mandatory("kstoken", kstoken);

        User user = new User(userId, TokenDTO.fromTokenString(kstoken));
        return this.fileInfoForFile(user, fileId);
    }

    @Override
    public FileInfo fileInfoForFile(User user, String fileId) {
        try (IndexClient indexClient = getIndexClient(user)) {
            return indexClient.getFileInfoForFile(fileId);
        }
    }

    @GET
    @Path("/{userId}/files/{fileId}/thumbnail")
    public String thumbnailPathForFileRS( //
            @PathParam("userId") Long userId, // 
            @PathParam("fileId") String fileId, //
            @QueryParam("kstoken") String kstoken) {
        mandatory("fileId", fileId);
        mandatory("kstoken", kstoken);

        User user = new User(userId, TokenDTO.fromTokenString(kstoken));
        return this.thumbnailPathForFile(user, fileId);
    }

    @Override
    public String thumbnailPathForFile(User user, String fileId) {
        try (IndexClient indexClient = getIndexClient(user)) {
            return indexClient.getThumbnailPathForFile(fileId);
        }
    }

    @Override
    public String delete( //
            User user, //
            Long jobId, //  
            Date timestamp) { // optional

        try (IndexClient indexClient = getIndexClient(user)) {
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
            @PathParam("userId") Long userId, //
            @QueryParam("job") Long jobId, // delete either via user and timestamp OR
            @QueryParam("time") Long timestamp, //
            @QueryParam("document") UUID indexFragmentUUID, //delete via document UUID
            @QueryParam("kstoken") String kstoken) {
        mandatory("kstoken", kstoken);

        User user = new User(userId, TokenDTO.fromTokenString(kstoken));

        //scenario1: delete record for user via job and timestamp
        if ((jobId != null) && (jobId != 0)) {
            mandatory("job", jobId);
            mandatory("time", timestamp);
            return status(Response.Status.ACCEPTED, delete(user, jobId, new Date(timestamp)));
        }
        if ((timestamp != null) && (timestamp != 0)) {
            mandatory("job", jobId);
            mandatory("time", timestamp);
            return status(Response.Status.ACCEPTED, delete(user, jobId, new Date(timestamp)));
        }

        //scenario2: delete record fors user via document uuid
        if (indexFragmentUUID != null) {
            mandatory("document", indexFragmentUUID);
            return status(Response.Status.ACCEPTED, delete(user, indexFragmentUUID));
        }

        //scenario3: no parameters used, delete entire index for user
        return status(Response.Status.ACCEPTED, delete(user, null, null));

    }

    @Override
    public String index( //
            User userId, //
            IndexDocument document) throws IOException {

        //TODO AL: We need to get the backupjob key (not user key) here for keyserver authentication
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
