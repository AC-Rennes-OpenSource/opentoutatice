/**
 * 
 */
package fr.toutatice.ecm.plarform.web.filemanager.zip;


/**
 * @author david
 */
public class ToutaticeZipLimitException extends Exception {

    private static final long serialVersionUID = -7504085483888234815L;
    
    public static final String MSG = "Zip do not leave enough space on tmp folder (limit: %s) - build aborted";
    public static final String MSG_PROP = "Property %s is not well defined: [%s]. Must be of the form <number>% - Zip build aborted";
    
    public ToutaticeZipLimitException(String msg) {
        super(msg);
    }
    
    public static ToutaticeZipLimitException limit(String percentSize) {
        return new ToutaticeZipLimitException(String.format(MSG, percentSize));
    }
    
    public static ToutaticeZipLimitException property(String prop) {
        return new ToutaticeZipLimitException(String.format(MSG_PROP, ToutaticeZipExporterUtils.MAX_SIZE_PROP, prop));
    }

}
