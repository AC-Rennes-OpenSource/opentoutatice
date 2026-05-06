/**
 * 
 */
package fr.toutatice.ecm.platform.automation.document;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.runtime.api.Framework;


/**
 * @author david
 *
 */
@Operation(id = AnyBlobToPDF.ID)
public class AnyBlobToPDF {

    /** Logger. */
    private static final Log log = LogFactory.getLog(AnyBlobToPDF.class);
    /** specific Logger */ 
    private static final Log sofficelog = LogFactory.getLog("soffice");
    /** current conversions in progress */
    private static Map<String, Long> conversionsInProgress = new ConcurrentHashMap<String,Long>();
    /** time to wait before a new conversion */
    private static Long retryTime = null;
    
    /** Operation's ID. */
    public static final String ID = "Blob.AnyToPDF";
    
    

    @Context
    protected ConversionService service;

    @Param(name = "converterName", required = false)
    protected String converterName = "toutaticeAny2pdf";

    /**
     * Convert blob of document to pdf with given converter.
     * Converter any2pdf is used by default.
     * 
     * @param doc
     * @return document
     * @throws Exception
     */
    @OperationMethod
    public Blob run(DocumentModel doc) throws Exception {
        BlobHolder bh = doc.getAdapter(BlobHolder.class);
        if (bh == null) {
            return null;
        }
        if ("application/pdf".equals(bh.getBlob().getMimeType())) {
            return bh.getBlob();
        }

        // Result
        Blob result = null;

        // Get modified date to compute cache key
        Calendar modDate = (GregorianCalendar) doc.getPropertyValue("dc:modified");
        Map<String, Serializable> cacheKeyParams = new HashMap<String, Serializable>();
        cacheKeyParams.put("modifiedOn", modDate.getTimeInMillis());

        long startConversionDate = new Date().getTime();
        
        // LBI #1918 - Don't run multiple conversions on the same document.
        if(AnyBlobToPDF.checkCurrentConversions(doc)) {
        	return null;
        }
        
        if(sofficelog.isDebugEnabled()) {
        	sofficelog.debug("Start conversion of "+doc.getTitle() + " " +doc.getPath());
        }
        BlobHolder pdfBh = null;
        try {
        	pdfBh = this.service.convert(this.converterName, bh, cacheKeyParams);
        }
        catch(ConversionException e) {
        	sofficelog.error(e);
        }
        
        // LBI #1852 - tracking conversion problems.
        if(pdfBh == null || pdfBh.getBlob() == null) {
        	long elapsed = new Date().getTime() - startConversionDate;
        	
        	sofficelog.warn("Unable to convert "+doc.getTitle() + " "+doc.getPath()+" (elapsed time : "+elapsed+ " ms.) ");
        	
        	return null;
        }
        else if(sofficelog.isDebugEnabled()) {
        	long elapsed = new Date().getTime() - startConversionDate;

        	sofficelog.debug("End of conversion of "+doc.getTitle() + " " +doc.getPath() + " (elapsed time : "+elapsed+ " ms.) ");
        }
        
        
        conversionsInProgress.remove(doc.getId());

        result = pdfBh.getBlob();

        return adjustBlobName(result, bh);
    }

    private Blob adjustBlobName(Blob result, BlobHolder bh) {
        String fname = result.getFilename();
        String filename = bh.getBlob().getFilename();
        if (filename != null && !filename.isEmpty()) {
            // add pdf extension
            int pos = filename.lastIndexOf('.');
            if (pos > 0) {
                filename = filename.substring(0, pos);
            }
            filename += ".pdf";
            result.setFilename(filename);
        } else if (fname != null && !fname.isEmpty()) {
            result.setFilename(fname);
        } else {
            result.setFilename("file");
        }

        result.setMimeType("application/pdf");

        return result;
    }

    @OperationMethod
    public Blob run(Blob blob) throws Exception {
        if ("application/pdf".equals(blob.getMimeType())) {
            return blob;
        }
        long startConversionDate = new Date().getTime();
        BlobHolder bh = new SimpleBlobHolder(blob);
        BlobHolder pdfBh = null;
        try {
            pdfBh = this.service.convert(this.converterName, bh, new HashMap<String, Serializable>());
        } catch(ConversionException e) {
            sofficelog.error(e);
        }
        if(pdfBh == null || pdfBh.getBlob() == null) {
            long elapsed = new Date().getTime() - startConversionDate;
            sofficelog.warn("Unable to convert "+ blob.getFilename() +" (elapsed time : "+elapsed+ " ms.) ");
            return null;
        }
        else if(sofficelog.isDebugEnabled()) {
            long elapsed = new Date().getTime() - startConversionDate;
            sofficelog.debug("End of conversion of "+ blob.getFilename() + " (elapsed time : "+elapsed+ " ms.) ");
        }
        return adjustBlobName(pdfBh.getBlob(), bh);
    }
    
    /**
     * Retry on failure management.
     * If document is marked in the map (failed or currently in progress), it is not generated
     * until the retry time has passed. 
     * 
     * @param doc
     * @return 
     */
    private static synchronized boolean checkCurrentConversions(DocumentModel doc)  {
    	boolean isInProgress = conversionsInProgress.containsKey(doc.getId());
    	
    	if(!isInProgress) {
    		conversionsInProgress.put(doc.getId(), new Date().getTime());
    	}
    	else {
    		
    		Long started = conversionsInProgress.get(doc.getId());
    		Long elapsed = new Date().getTime() - started;
    		    		
    		if(elapsed > getRetryTime()) {
    			conversionsInProgress.put(doc.getId(), new Date().getTime());
    			isInProgress = false;
    		}
    		else {
    			sofficelog.warn(doc.getTitle() + " "+doc.getPath()+" is currently in conversion, since "+elapsed+"ms.");
    		}
    		
    	}
    	
    	return isInProgress;
    }
    
    /**
     * get retry time param
     * @return retry time param
     */
    private static Long getRetryTime() {
    	if(retryTime == null) {
    		String property = Framework.getProperty("ottc.converter.toutaticeAny2pdf.retryTime");
    		if(property == null) {
    			property = "1800";
    		}
    		
    		retryTime = Long.parseLong(property) * 1000;
    	}
    	return retryTime;
    }

}
