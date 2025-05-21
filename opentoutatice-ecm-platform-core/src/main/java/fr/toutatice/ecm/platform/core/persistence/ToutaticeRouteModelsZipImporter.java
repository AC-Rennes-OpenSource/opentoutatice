package fr.toutatice.ecm.platform.core.persistence;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.ZipFile;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.io.DocumentPipe;
import org.nuxeo.ecm.core.io.DocumentReader;
import org.nuxeo.ecm.core.io.DocumentWriter;
import org.nuxeo.ecm.core.io.ExportedDocument;
import org.nuxeo.ecm.core.io.impl.DocumentPipeImpl;
import org.nuxeo.ecm.core.io.impl.plugins.DocumentModelWriter;
import org.nuxeo.ecm.core.io.impl.plugins.NuxeoArchiveReader;
import org.nuxeo.ecm.platform.filemanager.service.extension.ExportedZipImporter;
import org.nuxeo.ecm.platform.types.TypeManager;

public class ToutaticeRouteModelsZipImporter extends ExportedZipImporter {

	private static final long serialVersionUID = -3660849547853979447L;
	
	private static final Log log = LogFactory.getLog(ToutaticeRouteModelsZipImporter.class);
	
	private static final String[] ALLOWED_TYPES = {"DocumentRoute", "StepFolder", "DocumentRouteStep",
	        "ConditionalStepFolder", "RouteNode"};

    @Override
    public DocumentModel create(CoreSession session, Blob content, String path,
            boolean overwrite, String filename, TypeManager typeService)
            throws ClientException, IOException {

        File tmp = File.createTempFile("xml-importer", null);
        content.transferTo(tmp);
        ZipFile zip = getArchiveFileIfValid(tmp);

        if (zip == null) {
            tmp.delete();
            return null;
        }

        boolean overWrite = false;
        DocumentReader reader = new NuxeoArchiveReader(tmp);
        ExportedDocument root = reader.read();
        if(!Arrays.asList(ALLOWED_TYPES).contains(root.getType())) {
            reader.close();
            tmp.delete();
            return null;
        }
        PathRef rootRef = new PathRef(path, root.getPath().toString());
        ACP currentRouteModelACP = null;
        if (session.exists(rootRef)) {
            DocumentModel target = session.getDocument(rootRef);
            if (target.getPath().removeLastSegments(1).equals(new Path(path))) {
                overWrite = true;
                // clean up existing route before import
                DocumentModel routeModel = session.getDocument(rootRef);
                currentRouteModelACP = routeModel.getACP();
                session.removeDocument(rootRef);
            }
            
        }

        DocumentWriter writer = new DocumentModelWriter(session, path, 10);
        reader.close();
        reader = new NuxeoArchiveReader(tmp);

        DocumentRef resultingRef;
        if (overwrite && overWrite) {
            resultingRef = rootRef;
        } else {
            String rootName = root.getPath().lastSegment();
            resultingRef = new PathRef(path, rootName);
        }

        try {
            DocumentPipe pipe = new DocumentPipeImpl(10);
            pipe.setReader(reader);
            pipe.setWriter(writer);
            pipe.run();
        } catch (IllegalArgumentException e) {
            log.error("Can not import route model", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("interrupted", e);
        } catch (RuntimeException e) {
            log.error("Can not import route model", e);
            throw e;
        } catch (Exception e) {
            log.error("Can not import route model", e);
            throw new RuntimeException(e);
        } finally {
            reader.close();
            writer.close();
        }
        tmp.delete();
        DocumentModel newRouteModel = session.getDocument(resultingRef);
        if (currentRouteModelACP != null && overwrite && overWrite) {
            newRouteModel.setACP(currentRouteModelACP, true);
        }
        return session.saveDocument(newRouteModel);
    }
}
