package org.backmeup.index.rest.resources;

import java.io.IOException;
import java.util.Set;

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

import org.backmeup.data.dummy.ElasticSearchIndexClient;
import org.backmeup.index.api.IndexClient;
import org.backmeup.index.api.IndexServer;
import org.backmeup.index.model.FileInfo;
import org.backmeup.index.model.FileItem;
import org.backmeup.index.model.IndexDocument;
import org.backmeup.index.model.SearchResultAccumulator;

@Path("index")
@Produces(MediaType.APPLICATION_JSON)
public class Index implements IndexServer {

    protected IndexClient getIndexClient(Long userId) {
        return new ElasticSearchIndexClient(userId);
    }

    @Override
    @GET
    @Path("/{userId}")
    public SearchResultAccumulator query( //
            @PathParam("userId") Long userId, // 
            @QueryParam("query") String query, //
            @QueryParam("source") String filterBySource, //
            @QueryParam("type") String filterByType, //
            @QueryParam("job") String filterByJob, //
            @QueryParam("username") String username) {
        mandatory("query", query);
        mandatory("username", username);

        try (IndexClient indexClient = getIndexClient(userId)) {

            return indexClient.queryBackup(query, filterBySource, filterByType, filterByJob, username);

        }
    }

    @Override
    @GET
    @Path("/{userId}/files")
    public Set<FileItem> filesForJob( //
            @PathParam("userId") Long userId, // 
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
            @PathParam("userId") Long userId, // 
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
            @PathParam("userId") Long userId, // 
            @PathParam("fileId") String fileId) {
        mandatory("fileId", fileId);

        try (IndexClient indexClient = getIndexClient(userId)) {

            return indexClient.getThumbnailPathForFile(fileId);

        }
    }

    @Override
    public String delete( //
            Long userId, //
            Long jobId, //  
            Long timestamp) { // optional

        try (IndexClient indexClient = getIndexClient(userId)) {

            if (jobId != null && jobId != 0) {
                indexClient.deleteRecordsForJobAndTimestamp(jobId, timestamp);
                return "records of job deleted";
            }

            indexClient.deleteRecordsForUser();
            return "records of user deleted";

        }
    }

    @DELETE
    @Path("/{userId}")
    public Response deleteRS( //
            @PathParam("userId") Long userId, //
            @QueryParam("job") Long jobId, // optional for user and timestamp 
            @QueryParam("time") Long timestamp) { // optional
        
        return status(Response.Status.ACCEPTED, delete(userId, jobId, timestamp));
    }

    @Override
    public String index( //
            @PathParam("userId") Long userId, //
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
            @PathParam("userId") Long userId, //
            IndexDocument document) throws IOException {
        
        return status(Response.Status.CREATED, index(userId, document));
    }
    
    private void mandatory(String name, String value) {
        if (value == null) {
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
