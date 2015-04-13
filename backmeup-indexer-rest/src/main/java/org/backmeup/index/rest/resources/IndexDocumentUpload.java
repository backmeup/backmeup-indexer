package org.backmeup.index.rest.resources;

import java.io.IOException;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.backmeup.index.api.IndexDocumentUploadServer;
import org.backmeup.index.api.IndexFields;
import org.backmeup.index.model.IndexDocument;
import org.backmeup.index.model.User;
import org.backmeup.index.sharing.execution.IndexDocumentDropOffQueue;

@Path("upload")
@Produces(MediaType.APPLICATION_JSON)
public class IndexDocumentUpload implements IndexDocumentUploadServer {

    @Inject
    private IndexDocumentDropOffQueue droppOffQueue;

    @Override
    public String uploadForSharing(User userId, IndexDocument document) throws IOException {
        this.droppOffQueue.addIndexDocument(document);
        return "document uploaded for further processing";
    }

    @POST
    @Path("/{userId}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response uploadForSharingRS( //
            @PathParam("userId") User userId, //
            IndexDocument document) throws IOException {
        mandatory(document);
        return status(Response.Status.CREATED, uploadForSharing(userId, document));
    }

    private void mandatory(IndexDocument doc) {
        if (doc == null || (!doc.getFields().containsKey(IndexFields.FIELD_OWNER_ID))) {
            badRequestMissingIndexDocument();
        }
    }

    private void badRequestMissingIndexDocument() {
        throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST). //
                entity("IndexDocument is missing or missing mandatory fields"). //
                build());
    }

    private Response status(Response.Status code, String message) {
        return Response.status(code).entity(message).build();
    }

}
