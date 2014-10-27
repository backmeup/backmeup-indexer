package org.backmeup.index.rest.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.backmeup.index.client.IndexClient;
import org.backmeup.index.client.IndexClientFactory;
import org.backmeup.index.model.SearchResultAccumulator;

@Path("index")
public class Index {

    protected IndexClient getIndexClient(Long userId) {
        return new IndexClientFactory().getIndexClient(userId);
    }

    @GET
    @Path("/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    public SearchResultAccumulator queryBackup(//
            @PathParam("userId") Long userId, // 
            @QueryParam("query") String query, //
            @QueryParam("source") String source, //
            @QueryParam("type") String type, //
            @QueryParam("job") String job, //
            @QueryParam("username") String username) {

        try (IndexClient indexClient = getIndexClient(userId)) {

            return indexClient.queryBackup(query, source, type, job, username);

        }
    }

}
