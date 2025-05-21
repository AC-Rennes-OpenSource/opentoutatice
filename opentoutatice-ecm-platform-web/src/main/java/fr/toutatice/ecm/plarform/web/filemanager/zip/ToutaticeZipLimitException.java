/**
 * 
 */
package fr.toutatice.ecm.plarform.web.filemanager.zip;

import org.nuxeo.runtime.api.Framework;

/**
 * @author david
 */
public class ToutaticeZipLimitException extends Exception {

    private static final long serialVersionUID = -7504085483888234815L;
    
    public static final String MSG = "Zip do not leave enough space on tmp folder (limit: %s%%) - build aborted";
    
    public ToutaticeZipLimitException() {
        super(String.format(MSG, Framework.getProperty(ToutaticeZipExporterUtils.MAX_SIZE_PROP)));
    }

}
