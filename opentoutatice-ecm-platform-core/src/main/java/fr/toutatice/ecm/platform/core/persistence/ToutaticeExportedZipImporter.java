package fr.toutatice.ecm.platform.core.persistence;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipFile;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
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

public class ToutaticeExportedZipImporter extends ExportedZipImporter {

	private static final long serialVersionUID = -3660849547853979447L;
	
	private static final Log log = LogFactory.getLog(ToutaticeExportedZipImporter.class);

    @Override
    public DocumentModel create(CoreSession documentManager, Blob content,
            String path, boolean overwrite, String filename,
            TypeManager typeService) throws ClientException, IOException {

        File tmp = File.createTempFile("xml-importer", null);

        content.transferTo(tmp);

        ZipFile zip = getArchiveFileIfValid(tmp);

        if (zip == null) {
            tmp.delete();
            return null;
        }

        boolean importWithIds = false;
        DocumentReader reader = new ToutaticeNuxeoArchiveReader(tmp);
        ExportedDocument root = reader.read();
        IdRef rootRef = new IdRef(root.getId());

        if (documentManager.exists(rootRef)) {
            DocumentModel target = documentManager.getDocument(rootRef);
            if (target.getPath().removeLastSegments(1).equals(new Path(path))) {
                importWithIds = true;
            }
        }

        DocumentWriter writer = new ToutaticeDocumentModelWriter(documentManager, path,
                10);
        reader.close();
        reader = new ToutaticeNuxeoArchiveReader(tmp);

        DocumentRef resultingRef;
        if (overwrite && importWithIds) {
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
        } catch (Exception e) {
            log.warn(e, e);
        } finally {
            reader.close();
            writer.close();
        }
        tmp.delete();
        return documentManager.getDocument(resultingRef);
    }
}
