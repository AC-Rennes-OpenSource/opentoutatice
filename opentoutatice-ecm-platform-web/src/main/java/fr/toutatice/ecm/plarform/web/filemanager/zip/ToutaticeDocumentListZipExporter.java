/**
 * 
 */
package fr.toutatice.ecm.plarform.web.filemanager.zip;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.webapp.clipboard.DocumentListZipExporter;
import org.nuxeo.runtime.api.Framework;

/**
 * @author david
 */
public class ToutaticeDocumentListZipExporter extends DocumentListZipExporter {
    
    private static final Log log = LogFactory.getLog(ToutaticeDocumentListZipExporter.class);
    
    protected static final int BUFFER = 2048;
    
    protected static final String SUMMARY_FILENAME = "INDEX.txt";
    
    public ToutaticeDocumentListZipExporter() {
        super();
    }
    
    public File exportWorklistAsZip(List<DocumentModel> documents, CoreSession documentManager, boolean exportAllBlobs)
            throws ClientException, IOException {
        StringBuilder blobList = new StringBuilder();

        File tmpFile = File.createTempFile("NX-BigZipFile-", ".zip");
        tmpFile.deleteOnExit(); // file is deleted after being downloaded in
                                // DownloadServlet
        FileOutputStream fout = new FileOutputStream(tmpFile);
        ZipOutputStream out = new ZipOutputStream(fout);
        out.setMethod(ZipOutputStream.DEFLATED);
        out.setLevel(9);
        byte[] data = new byte[BUFFER];

        for (DocumentModel doc : documents) {

            // first check if DM is attached to the core
            if (doc.getSessionId() == null) {
                // refetch the doc from the core
                doc = documentManager.getDocument(doc.getRef());
            }

            // NXP-2334 : skip deleted docs
            if (LifeCycleConstants.DELETED_STATE.equals(doc.getCurrentLifeCycleState())) {
                continue;
            }
            
            try {
                BlobHolder bh = doc.getAdapter(BlobHolder.class);
                if (doc.isFolder() && !isEmptyFolder(doc, documentManager)) {
                    addFolderToZip("", out, doc, data, documentManager, blobList, exportAllBlobs);
                } else if (bh != null) {
                    addBlobHolderToZip("", out, doc, data, blobList, bh, exportAllBlobs);
                }
            } catch (ToutaticeZipLimitException | ToutaticeZipFormatException ze) {
                try {
                    out.close();
                    fout.close();
                    
                    if(tmpFile != null) {
                        tmpFile.delete();
                    }
                } catch (ZipException e) {
                    return null;
                }
                throw new ClientException(ze);
            } 
        }
        if (blobList.length() > 1) {
            addSummaryToZip(out, data, blobList);
        }
        try {
            out.close();
            fout.close();
        } catch (ZipException e) {
            return null;
        }
        return tmpFile;
    }
    
    protected boolean isEmptyFolder(DocumentModel doc, CoreSession documentManager) throws ClientException {

        List<DocumentModel> docList = documentManager.getChildren(doc.getRef());
        for (DocumentModel docChild : docList) {
            // If there is a blob or a folder, it is not empty.
            if (docChild.getAdapter(BlobHolder.class) != null || docChild.isFolder()) {
                return false;
            }
        }
        return true;
    }
    
    protected void addFolderToZip(String path, ZipOutputStream out, DocumentModel doc, byte[] data,
            CoreSession documentManager, StringBuilder blobList, boolean exportAllBlobs) throws ClientException,
            IOException, ToutaticeZipLimitException, ToutaticeZipFormatException {
      
        String title = doc.getTitle();
        List<DocumentModel> docList = documentManager.getChildren(doc.getRef());
        for (DocumentModel docChild : docList) {
            // NXP-2334 : skip deleted docs
            if (LifeCycleConstants.DELETED_STATE.equals(docChild.getCurrentLifeCycleState())) {
                continue;
            }
            BlobHolder bh = docChild.getAdapter(BlobHolder.class);
            String newPath = null;
            if (path.length() == 0) {
                newPath = title;
            } else {
                newPath = path + "/" + title;
            }
            if (docChild.isFolder() && !isEmptyFolder(docChild, documentManager)) {
                addFolderToZip(newPath, out, docChild, data, documentManager, blobList, exportAllBlobs);
            } else if (bh != null) {
                addBlobHolderToZip(newPath, out, docChild, data, blobList, bh, exportAllBlobs);
            }
        }
    }
    
    private void addBlobHolderToZip(String path, ZipOutputStream out, DocumentModel doc, byte[] data, StringBuilder blobList, BlobHolder bh,
            boolean exportAllBlobs) throws IOException, ClientException, ToutaticeZipLimitException, ToutaticeZipFormatException {

        List<Blob> blobs = new ArrayList<Blob>();

        if (exportAllBlobs) {
            if (bh.getBlobs() != null) {
                blobs = bh.getBlobs();
            }
        } else {
            Blob mainBlob = bh.getBlob();
            if (mainBlob != null) {
                blobs.add(mainBlob);
            }
        }

        if (blobs.size() > 0) { // add document info
            SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            if (path.length() > 0) {
                blobList.append(path).append('/');
            }
            blobList.append(doc.getTitle()).append(" ");
            blobList.append(doc.getType()).append(" ");

            Calendar c = (Calendar) doc.getPropertyValue("dc:modified");
            if (c != null) {
                blobList.append(format.format(c.getTime()));
            }
            blobList.append("\n");
        }

        for (Blob content : blobs) {
            String fileName = content.getFilename();
            if (fileName == null) {
                // use a default value
                fileName = "file.bin";
            }
            BufferedInputStream buffi = new BufferedInputStream(content.getStream(), BUFFER);

            // Workaround to deal with duplicate file names.
            int tryCount = 0;
            String entryPath = null;
            String entryName = null;
            while (true) {
                try {
                    ZipEntry entry = null;
                    if (tryCount == 0) {
                        entryName = fileName;
                    } else {
                        entryName = formatFileName(fileName, "(" + tryCount + ")");
                    }
                    if (path.length() == 0) {
                        entryPath = entryName;
                    } else {
                        entryPath = path + "/" + entryName;
                    }
                    entryPath = escapeEntryPath(entryPath);
                    entry = new ZipEntry(entryPath);
                    out.putNextEntry(entry);
                    break;
                } catch (ZipException e) {
                    tryCount++;
                }
            }
            blobList.append(" - ").append(entryName).append("\n");


            int count = buffi.read(data, 0, BUFFER);
            while (count != -1) {
                out.write(data, 0, count);
                if (!ToutaticeZipExporterUtils.stillTmpSpaceLeft()) {
                    out.closeEntry();
                    buffi.close();
                    throw new ToutaticeZipLimitException();
                }
                count = buffi.read(data, 0, BUFFER);
            }
            out.closeEntry();
            buffi.close();
        }
    }
    
    protected String formatFileName(String filename, String count) {
        StringBuilder sb = new StringBuilder();
        CharSequence name = filename.subSequence(0, filename.lastIndexOf("."));
        CharSequence extension = filename.subSequence(filename.lastIndexOf("."), filename.length());
        sb.append(name).append(count).append(extension);
        return sb.toString();
    }
    
    /**
     * Writes a summary file and puts it in the archive.
     */
    protected void addSummaryToZip(ZipOutputStream out, byte[] data, StringBuilder sb) throws IOException {

        Blob content = new StringBlob(sb.toString());

        BufferedInputStream buffi = new BufferedInputStream(content.getStream(), BUFFER);

        ZipEntry entry = new ZipEntry(SUMMARY_FILENAME);
        out.putNextEntry(entry);
        int count = buffi.read(data, 0, BUFFER);

        while (count != -1) {
            out.write(data, 0, count);
            count = buffi.read(data, 0, BUFFER);
        }
        out.closeEntry();
        buffi.close();
    }

}
