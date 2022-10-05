/**
 * 
 */
package fr.toutatice.ecm.platform.core.listener;

import fr.toutatice.ecm.platform.core.helper.ToutaticeDocumentHelper;
import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.impl.primitives.BlobProperty;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;


/**
 * @author Loïc Billon
 *
 */
public class ToutaticeSyncBinaryNameListener implements EventListener {

    @Override
    public void handleEvent(Event event) throws ClientException {
        
        if(event.getContext() instanceof DocumentEventContext) {

            DocumentEventContext docCtx = (DocumentEventContext) event.getContext();

            DocumentModel document = docCtx.getSourceDocument();

            if (ToutaticeDocumentHelper.isInWorkspaceLike(docCtx.getCoreSession(), document)
                    && ToutaticeDocumentEventListenerHelper.isAlterableDocument(document)) {

                // if document has a file attached
                if(document.hasSchema("file") && document.getPropertyValue("file:content") != null) {

                    String initialDcTitle = (String) document.getPropertyValue("dc:title");
                    BlobProperty fileContent = (BlobProperty) document.getProperty("file:content");
                    Blob blob = (Blob) fileContent.getValue();
                    String initialFilename = blob.getFilename();

                    // get initial binary title, compare to dc:title
                    if(!StringUtils.equalsIgnoreCase(initialDcTitle, initialFilename)) {

                        // if different
                        // extract extension from current file
                        String extension = StringUtils.substringAfterLast(initialFilename, ".");

                        // Add this extension to the new title
                        if(StringUtils.isNotEmpty(extension)) {
                            extension = ".".concat(extension);
                            String newFileName = StringUtils.substringBeforeLast(initialDcTitle, extension);
                            newFileName = newFileName.concat(extension);

                            // update the blob
                            blob.setFilename(newFileName);
                            fileContent.setValue(blob);
                        }

                    }
                }
            }
        }

    }

}
