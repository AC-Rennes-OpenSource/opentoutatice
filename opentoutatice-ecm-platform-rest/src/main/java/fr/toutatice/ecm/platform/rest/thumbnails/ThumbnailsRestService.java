package fr.toutatice.ecm.platform.rest.thumbnails;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.*;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.jaxrs.session.SessionFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 * @author Loïc Billon
 *
 */
@Path("/thumbnail/")
public class ThumbnailsRestService {

    private static final Log log = LogFactory.getLog(ThumbnailsRestService.class);

    @GET
    @Path("{uuid}")
    public Response getThumbnail(@PathParam("uuid") String uuid) {

        try {
            CoreSession session = CoreInstance.openCoreSession("default", "Administrator");

            DocumentModel doc = session.getDocument(new IdRef(uuid));

//
//            if(!list.isEmpty()) {
//                DocumentModel doc = list.get(0);
//
//                if("Workspace".equals(doc.getType())) {

                    Blob vignette = (Blob) doc.getPropertyValue("ttc:vignette");

                    if(vignette == null) {
                        BlobHolder bh = (BlobHolder)doc.getAdapter(BlobHolder.class);
                        if (bh != null) {
                            vignette = bh.getBlob();
                        }

                    }

                    return Response.ok(vignette, "image/jpeg").build();
//                }
//            }

        }
        catch(Exception e) {

            log.error(e.getMessage());
        }

        return Response
                .status(Response.Status.NOT_FOUND)
                .build();
    }

}
